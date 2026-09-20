package com.example.inventory.listener;

import com.example.common.event.BaseEvent;
import com.example.common.event.IdempotentEventProcessor;
import com.example.common.event.OrderEvent;
import com.example.common.event.ProductEvent;
import com.example.inventory.saga.InventorySagaOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InventoryEventListener {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventListener.class);

    private final IdempotentEventProcessor idempotentEventProcessor;
    private final InventorySagaOrchestrator inventorySagaOrchestrator;

    public InventoryEventListener(IdempotentEventProcessor idempotentEventProcessor,
                                   InventorySagaOrchestrator inventorySagaOrchestrator) {
        this.idempotentEventProcessor = idempotentEventProcessor;
        this.inventorySagaOrchestrator = inventorySagaOrchestrator;
    }

    @RabbitListener(queues = "${rabbitmq.queue.inventory-events}")
    @Transactional
    public void handleEvent(BaseEvent event) {
        if (event instanceof ProductEvent) {
            idempotentEventProcessor.process((ProductEvent) event, this::handleProductEventInternal);
        } else if (event instanceof OrderEvent) {
            idempotentEventProcessor.process((OrderEvent) event, this::handleOrderEventInternal);
        } else {
            log.debug("Unhandled event type: {}", event.getClass().getName());
        }
    }

    private void handleProductEventInternal(ProductEvent event) {
        log.info("Received product event: {}", event);

        switch (ProductEvent.EventType.valueOf(event.getEventType())) {
            case CREATED, VARIANT_CREATED -> handleProductOrVariantCreated(event);
            case UPDATED, VARIANT_UPDATED -> handleProductOrVariantUpdated(event);
            case DELETED, VARIANT_DELETED -> handleProductOrVariantDeleted(event);
        }
    }

    private void handleOrderEventInternal(OrderEvent event) {
        log.info("Received order event: {}", event);

        switch (OrderEvent.EventType.valueOf(event.getEventType())) {
            case CREATED -> inventorySagaOrchestrator.handleOrderCreated(event).subscribe();
            case CANCELLED -> inventorySagaOrchestrator.handleOrderCancelled(event).subscribe();
            default -> log.debug("Unhandled order event type: {}", event.getEventType());
        }
    }

    private void handleProductOrVariantCreated(ProductEvent event) {
        // This is handled by the orchestrator if needed, keeping existing logic here for product events
        log.info("Product event received, delegating to orchestrator if needed: {}", event.getEventType());
    }

    private void handleProductOrVariantUpdated(ProductEvent event) {
        log.info("Product event received, delegating to orchestrator if needed: {}", event.getEventType());
    }

    private void handleProductOrVariantDeleted(ProductEvent event) {
        log.info("Product event received, delegating to orchestrator if needed: {}", event.getEventType());
    }
}