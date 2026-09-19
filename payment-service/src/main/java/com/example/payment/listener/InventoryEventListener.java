package com.example.payment.listener;

import com.example.common.event.InventoryEvent;
import com.example.common.event.ReactiveIdempotentEventProcessor;
import com.example.common.listener.BaseReactiveSagaListener;
import com.example.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Component
public class InventoryEventListener extends BaseReactiveSagaListener<InventoryEvent> {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventListener.class);

    private final PaymentService paymentService;

    public InventoryEventListener(ReactiveIdempotentEventProcessor idempotentEventProcessor,
                                   PaymentService paymentService) {
        super(idempotentEventProcessor);
        this.paymentService = paymentService;
    }

    @RabbitListener(queues = "${rabbitmq.queue.inventory-reserved}")
    public void handleInventoryReserved(InventoryEvent event) {
        processEvent(event);
    }

    @Override
    protected Mono<Void> handleEventInternal(InventoryEvent event) {
        log.info("Received inventory reserved event: variantId={}, reserved={}, backordered={}, eventId={}",
                event.getVariantId(), event.getReserved(), event.getBackordered(), event.getEventId());

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

        BigDecimal amount = BigDecimal.valueOf(reserved * 100);
        String idempotencyKey = "inventory-reserved-" + event.getEventId();

        return paymentService.authorizePayment(
                        event.getVariantId(),
                        amount,
                        "USD",
                        "customer-" + event.getVariantId(),
                        "customer-" + event.getVariantId() + "@example.com",
                        idempotencyKey
                )
                .doOnNext(payment -> log.info("Payment authorized for inventory reservation: paymentId={}, variantId={}",
                        payment.id(), event.getVariantId()))
                .doOnError(error -> log.error("Failed to authorize payment for inventory reservation: variantId={}, error={}",
                        event.getVariantId(), error.getMessage()))
                .then();
    }
}