package com.example.order.listener;

import com.example.common.event.InventoryEvent;
import com.example.common.event.ReactiveIdempotentEventProcessor;
import com.example.common.listener.BaseReactiveSagaListener;
import com.example.order.service.OrderSagaOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class InventoryEventListener extends BaseReactiveSagaListener<InventoryEvent> {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventListener.class);

    private final OrderSagaOrchestrator sagaOrchestrator;

    public InventoryEventListener(ReactiveIdempotentEventProcessor idempotentEventProcessor,
                                   OrderSagaOrchestrator sagaOrchestrator) {
        super(idempotentEventProcessor);
        this.sagaOrchestrator = sagaOrchestrator;
    }

    @RabbitListener(queues = "${rabbitmq.queue.inventory-events}")
    public void handleInventoryEvent(InventoryEvent event) {
        processEvent(event);
    }

    @Override
    protected Mono<Void> handleEventInternal(InventoryEvent event) {
        log.info("Received inventory event: eventType={}, eventId={}, variantId={}, reserved={}, backordered={}",
                event.getEventType(), event.getEventId(), event.getVariantId(), event.getReserved(), event.getBackordered());

        if (event.getVariantId() == null) {
            log.warn("Inventory event missing variantId, skipping: eventId={}", event.getEventId());
            return Mono.empty();
        }

        return switch (InventoryEvent.EventType.valueOf(event.getEventType())) {
            case RESERVED -> sagaOrchestrator.handleInventoryReserved(event);
            case RELEASED -> sagaOrchestrator.handleInventoryReleased(event);
            case CONFIRMED -> {
                log.info("Stock confirmed for variant: {}", event.getVariantId());
                yield Mono.empty();
            }
            default -> {
                log.debug("Unhandled inventory event type: {}", event.getEventType());
                yield Mono.empty();
            }
        };
    }
}