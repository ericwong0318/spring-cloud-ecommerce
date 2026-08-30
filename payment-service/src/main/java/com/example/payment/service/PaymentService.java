package com.example.payment.service;

import com.example.common.dto.PaymentDto;
import com.example.common.event.OutboxEventPublisher;
import com.example.common.event.PaymentEvent;
import com.example.payment.mapper.PaymentMapper;
import com.example.payment.model.Payment;
import com.example.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final OutboxEventPublisher outboxEventPublisher;

    @Transactional
    public PaymentDto authorizePayment(Long orderId, BigDecimal amount, String currency, String customerId, String customerEmail) {
        log.info("Authorizing payment for order: {}", orderId);
        
        // Check for existing payment with same idempotency key
        String idempotencyKey = "auth-" + orderId + "-" + UUID.randomUUID().toString().substring(0, 8);
        if (paymentRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            log.warn("Duplicate authorization request for order: {}", orderId);
            throw new DuplicatePaymentException("Payment already authorized for this order");
        }

        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setAmount(amount);
        payment.setCurrency(currency);
        payment.setStatus(Payment.PaymentStatus.AUTHORIZED);
        payment.setIdempotencyKey(idempotencyKey);
        payment.setAuthorizedAt(LocalDateTime.now());
        
        Payment saved = paymentRepository.save(payment);
        
        // Publish PaymentEvent.SUCCESS to outbox
        PaymentEvent event = PaymentEvent.success(saved.getId(), orderId, customerId, customerEmail,
                amount, currency, saved.getGatewayTransactionId());
        outboxEventPublisher.saveEvent("Payment", saved.getId().toString(), "SUCCESS", event);
        log.info("Published PaymentEvent.SUCCESS to outbox for payment: {}", saved.getId());
        
        return paymentMapper.toDto(saved);
    }

    @Transactional
    public PaymentDto capturePayment(Long paymentId, String gatewayTransactionId) {
        log.info("Capturing payment: {}", paymentId);
        
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));
        
        if (payment.getStatus() != Payment.PaymentStatus.AUTHORIZED) {
            throw new IllegalStateException("Payment must be in AUTHORIZED status to capture. Current status: " + payment.getStatus());
        }
        
        payment.setStatus(Payment.PaymentStatus.CAPTURED);
        payment.setGatewayTransactionId(gatewayTransactionId);
        payment.setCapturedAt(LocalDateTime.now());
        
        Payment saved = paymentRepository.save(payment);
        
        // Publish PaymentEvent.SUCCESS for capture
        PaymentEvent event = PaymentEvent.success(saved.getId(), saved.getOrderId(), 
                null, null, saved.getAmount(), saved.getCurrency(), gatewayTransactionId);
        outboxEventPublisher.saveEvent("Payment", saved.getId().toString(), "SUCCESS", event);
        log.info("Published PaymentEvent.SUCCESS (capture) to outbox for payment: {}", saved.getId());
        
        return paymentMapper.toDto(saved);
    }

    @Transactional
    public PaymentDto refundPayment(Long paymentId, BigDecimal amount, String reason) {
        log.info("Refunding payment: {} amount: {}", paymentId, amount);
        
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));
        
        if (payment.getStatus() != Payment.PaymentStatus.CAPTURED && 
            payment.getStatus() != Payment.PaymentStatus.PARTIALLY_REFUNDED) {
            throw new IllegalStateException("Payment must be CAPTURED or PARTIALLY_REFUNDED to refund. Current status: " + payment.getStatus());
        }
        
        if (amount.compareTo(payment.getAmount()) > 0) {
            throw new IllegalArgumentException("Refund amount cannot exceed payment amount");
        }
        
        Payment.PaymentStatus newStatus = amount.compareTo(payment.getAmount()) == 0 
                ? Payment.PaymentStatus.REFUNDED 
                : Payment.PaymentStatus.PARTIALLY_REFUNDED;
        
        payment.setStatus(newStatus);
        payment.setRefundedAt(LocalDateTime.now());
        
        Payment saved = paymentRepository.save(payment);
        
        // Publish PaymentEvent.REFUNDED
        PaymentEvent event = PaymentEvent.refunded(saved.getId(), saved.getOrderId(), 
                null, null, amount, saved.getCurrency(), saved.getGatewayTransactionId());
        outboxEventPublisher.saveEvent("Payment", saved.getId().toString(), "REFUNDED", event);
        log.info("Published PaymentEvent.REFUNDED to outbox for payment: {}", saved.getId());
        
        return paymentMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public PaymentDto getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + id));
        return paymentMapper.toDto(payment);
    }

    @Transactional(readOnly = true)
    public PaymentDto getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .map(paymentMapper::toDto)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for order: " + orderId));
    }

    @Transactional(readOnly = true)
    public List<PaymentDto> getPaymentsByOrderId(Long orderId) {
        return paymentRepository.findByOrderIdAndStatus(orderId, Payment.PaymentStatus.AUTHORIZED).stream()
                .map(paymentMapper::toDto)
                .toList();
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