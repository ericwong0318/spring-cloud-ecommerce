package com.example.payment.saga;

import com.example.common.event.InventoryEvent;
import com.example.common.event.PaymentEvent;
import com.example.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Service
public class PaymentSagaOrchestratorImpl implements PaymentSagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(PaymentSagaOrchestratorImpl.class);

    private final PaymentService paymentService;

    public PaymentSagaOrchestratorImpl(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Override
    public Mono<Void> handleInventoryReserved(InventoryEvent event) {
        log.info("Payment saga orchestrator handling INVENTORY_RESERVED for variant: {}, reserved: {}, backordered: {}, orderId: {}",
                event.getVariantId(), event.getReserved(), event.getBackordered(), event.getOrderId());

        if (!InventoryEvent.EventType.RESERVED.name().equals(event.getEventType())) {
            log.debug("Ignoring non-RESERVED event type: {}", event.getEventType());
            return Mono.empty();
        }

        Integer reserved = event.getReserved();
        Integer backordered = event.getBackordered();

        if (reserved == null || reserved <= 0) {
            log.info("No stock reserved for variant {}, skipping payment authorization", event.getVariantId());
            return Mono.empty();
        }

        Long orderId = event.getOrderId();
        String customerId = event.getCustomerId();
        String customerEmail = event.getCustomerEmail();

        if (orderId == null || customerId == null || customerEmail == null) {
            log.warn("Missing orderId or customer info in inventory event, skipping payment authorization");
            return Mono.empty();
        }

        BigDecimal amount = BigDecimal.valueOf(reserved * 100);
        String idempotencyKey = "inventory-reserved-" + event.getEventId();

        return paymentService.authorizePayment(
                        orderId,
                        amount,
                        "USD",
                        customerId,
                        customerEmail,
                        idempotencyKey
                )
                .doOnNext(payment -> log.info("Payment authorized for inventory reservation: paymentId={}, orderId={}, variantId={}",
                        payment.id(), orderId, event.getVariantId()))
                .doOnError(error -> log.error("Failed to authorize payment for inventory reservation: orderId={}, variantId={}, error={}",
                        orderId, event.getVariantId(), error.getMessage()))
                .then();
    }

    @Override
    public Mono<Void> handlePaymentAuthorized(PaymentEvent event) {
        log.info("Payment saga orchestrator handling PAYMENT_AUTHORIZED for payment: {}", event.getPaymentId());
        return Mono.empty();
    }

    @Override
    public Mono<Void> handlePaymentCaptured(PaymentEvent event) {
        log.info("Payment saga orchestrator handling PAYMENT_CAPTURED for payment: {}", event.getPaymentId());
        return Mono.empty();
    }

    @Override
    public Mono<Void> handlePaymentFailed(PaymentEvent event) {
        log.info("Payment saga orchestrator handling PAYMENT_FAILED for payment: {}", event.getPaymentId());
        return Mono.empty();
    }

    @Override
    public Mono<Void> handlePaymentRefunded(PaymentEvent event) {
        log.info("Payment saga orchestrator handling PAYMENT_REFUNDED for payment: {}", event.getPaymentId());
        return Mono.empty();
    }
}