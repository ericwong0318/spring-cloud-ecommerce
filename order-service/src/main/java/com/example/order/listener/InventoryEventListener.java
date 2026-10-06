package com.example.order.listener;

import com.example.common.event.InventoryEvent;
import com.example.common.event.ReactiveIdempotentEventProcessor;
import com.example.common.listener.BaseReactiveSagaListener;
import com.example.commonsaga.OrderSagaHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class InventoryEventListener extends BaseReactiveSagaListener<InventoryEvent> {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventListener.class);

    private final OrderSagaHandler sagaHandler;

    public InventoryEventListener(ReactiveIdempotentEventProcessor idempotentEventProcessor,
                                   OrderSagaHandler sagaHandler) {
        super(idempotentEventProcessor);
        this.sagaHandler = sagaHandler;
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
            case RESERVED -> sagaHandler.handleInventoryReserved(event);
            case RELEASED -> sagaHandler.handleInventoryReleased(event);
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
