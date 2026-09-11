package com.example.payment.service;

import com.example.common.dto.PaymentDto;
import com.example.common.event.PaymentEvent;
import com.example.payment.domain.Payment;
import com.example.payment.event.PaymentEventPublisher;
import com.example.payment.gateway.MockPaymentGateway;
import com.example.payment.repository.PaymentRepository;
import com.example.payment.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final PaymentEventPublisher eventPublisher;
    private final MockPaymentGateway mockGateway;
    private final TransactionalOperator transactionalOperator;

    public PaymentService(PaymentRepository paymentRepository, ProcessedEventRepository processedEventRepository,
                          PaymentEventPublisher eventPublisher, MockPaymentGateway mockGateway,
                          TransactionalOperator transactionalOperator) {
        this.paymentRepository = paymentRepository;
        this.processedEventRepository = processedEventRepository;
        this.eventPublisher = eventPublisher;
        this.mockGateway = mockGateway;
        this.transactionalOperator = transactionalOperator;
    }

    public Mono<PaymentDto> authorizePayment(Long orderId, BigDecimal amount, String currency,
                                              String customerId, String customerEmail, String idempotencyKey) {
        log.info("Authorizing payment for order: {} with idempotency key: {}", orderId, idempotencyKey);

        return paymentRepository.findByIdempotencyKey(idempotencyKey)
                .flatMap(existing -> {
                    log.warn("Duplicate authorization request for order: {} with idempotency key: {}", orderId, idempotencyKey);
                    return Mono.<PaymentDto>error(new DuplicatePaymentException("Payment already authorized for this idempotency key"));
                })
                .switchIfEmpty(Mono.defer(() -> {
                    return mockGateway.authorize(amount, currency, idempotencyKey, null)
                            .flatMap(gatewayResponse -> {
                                if (!gatewayResponse.success()) {
                                    Payment failedPayment = Payment.authorize(orderId, amount, currency, idempotencyKey);
                                    Payment failed = failedPayment.withStatus(Payment.PaymentStatus.FAILED);
                                    return paymentRepository.save(failed)
                                            .flatMap(saved -> {
                                                PaymentEvent event = PaymentEvent.failed(saved.id(), orderId, customerId, customerEmail,
                                                        amount, currency);
                                                return eventPublisher.publish(event)
                                                        .thenReturn(saved);
                                            })
                                            .map(this::toDto);
                                }

                                Payment payment = Payment.authorize(orderId, amount, currency, idempotencyKey)
                                        .withGatewayTransactionId(gatewayResponse.transactionId());

                                return paymentRepository.save(payment)
                                        .flatMap(saved -> {
                                            PaymentEvent event = PaymentEvent.authorized(saved.id(), orderId, customerId, customerEmail,
                                                    amount, currency, saved.gatewayTransactionId());
                                            return eventPublisher.publish(event)
                                                    .thenReturn(saved);
                                        })
                                        .map(this::toDto);
                            });
                }))
                .as(transactionalOperator::transactional);
    }

    public Mono<PaymentDto> capturePayment(Long paymentId, String gatewayTransactionId, String idempotencyKey) {
        log.info("Capturing payment: {} with idempotency key: {}", paymentId, idempotencyKey);

        return paymentRepository.findById(paymentId)
                .switchIfEmpty(Mono.error(new PaymentNotFoundException("Payment not found: " + paymentId)))
                .flatMap(payment -> {
                    if (payment.status() == Payment.PaymentStatus.CAPTURED) {
                        log.info("Payment {} already captured, returning existing", paymentId);
                        return Mono.just(toDto(payment));
                    }
                    if (!payment.canCapture()) {
                        return Mono.error(new IllegalStateException("Payment must be in AUTHORIZED status to capture. Current status: " + payment.status()));
                    }

                    Payment updated = payment.capture(gatewayTransactionId);

                    return paymentRepository.save(updated)
                            .flatMap(saved -> {
                                PaymentEvent event = PaymentEvent.captured(saved.id(), saved.orderId(),
                                        null, null, saved.amount(), saved.currency(), gatewayTransactionId);
                                return eventPublisher.publish(event)
                                        .thenReturn(saved);
                            })
                            .map(this::toDto);
                })
                .as(transactionalOperator::transactional);
    }

    public Mono<PaymentDto> refundPayment(Long paymentId, BigDecimal amount, String reason, String idempotencyKey) {
        log.info("Refunding payment: {} amount: {} with idempotency key: {}", paymentId, amount, idempotencyKey);

        return paymentRepository.findByIdempotencyKey(idempotencyKey)
                .flatMap(existing -> {
                    log.warn("Duplicate refund request for payment: {} with idempotency key: {}", paymentId, idempotencyKey);
                    return getPaymentById(paymentId);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    return paymentRepository.findById(paymentId)
                            .switchIfEmpty(Mono.error(new PaymentNotFoundException("Payment not found: " + paymentId)))
                            .flatMap(payment -> {
                                if (!payment.canRefund()) {
                                    return Mono.error(new IllegalStateException("Payment must be CAPTURED or PARTIALLY_REFUNDED to refund. Current status: " + payment.status()));
                                }

                                if (!payment.isAmountValid(amount)) {
                                    return Mono.error(new IllegalArgumentException("Refund amount cannot exceed payment amount"));
                                }

                                Payment updated = payment.refund(amount);

                                return paymentRepository.save(updated)
                                        .flatMap(saved -> {
                                            Payment.PaymentStatus newStatus = saved.status();
                                            PaymentEvent event;
                                            if (newStatus == Payment.PaymentStatus.REFUNDED) {
                                                event = PaymentEvent.refunded(saved.id(), saved.orderId(),
                                                        null, null, amount, saved.currency(), saved.gatewayTransactionId());
                                            } else {
                                                event = PaymentEvent.partiallyRefunded(saved.id(), saved.orderId(),
                                                        null, null, amount, saved.currency(), saved.gatewayTransactionId());
                                            }
                                            return eventPublisher.publish(event)
                                                    .thenReturn(saved);
                                        })
                                        .map(this::toDto);
                            });
                }))
                .as(transactionalOperator::transactional);
    }

    public Mono<PaymentDto> getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .switchIfEmpty(Mono.error(new PaymentNotFoundException("Payment not found: " + id)))
                .map(this::toDto);
    }

    public Mono<PaymentDto> getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .switchIfEmpty(Mono.error(new PaymentNotFoundException("Payment not found for order: " + orderId)))
                .map(this::toDto);
    }

    public reactor.core.publisher.Flux<PaymentDto> getPaymentsByOrderId(Long orderId) {
        return paymentRepository.findByOrderIdAll(orderId)
                .map(this::toDto);
    }

    private PaymentDto toDto(Payment payment) {
        PaymentDto dto = new PaymentDto();
        dto.setId(payment.id());
        dto.setOrderId(payment.orderId());
        dto.setAmount(payment.amount());
        dto.setCurrency(payment.currency());
        dto.setStatus(PaymentDto.PaymentStatus.valueOf(payment.status().name()));
        dto.setGatewayTransactionId(payment.gatewayTransactionId());
        dto.setIdempotencyKey(payment.idempotencyKey());
        dto.setAuthorizedAt(payment.authorizedAt());
        dto.setCapturedAt(payment.capturedAt());
        dto.setRefundedAt(payment.refundedAt());
        dto.setCreatedAt(payment.createdAt());
        dto.setUpdatedAt(payment.updatedAt());
        return dto;
    }

    public static class PaymentNotFoundException extends RuntimeException {
        public PaymentNotFoundException(String message) {
            super(message);
        }
    }

    public static class DuplicatePaymentException extends RuntimeException {
        public DuplicatePaymentException(String message) {
            super(message);
        }
    }
}
