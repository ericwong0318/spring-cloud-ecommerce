package com.example.inventory.listener;

import com.example.common.event.BaseEvent;
import com.example.common.event.IdempotentEventProcessor;
import com.example.common.event.InventoryEvent;
import com.example.common.event.OrderEvent;
import com.example.common.event.ProductEvent;
import com.example.inventory.model.Inventory;
import com.example.inventory.repository.InventoryRepository;
import com.example.inventory.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InventoryEventListener {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventListener.class);

    private final InventoryRepository inventoryRepository;
    private final InventoryService inventoryService;
    private final IdempotentEventProcessor idempotentEventProcessor;

    public InventoryEventListener(InventoryRepository inventoryRepository,
                                   InventoryService inventoryService,
                                   IdempotentEventProcessor idempotentEventProcessor) {
        this.inventoryRepository = inventoryRepository;
        this.inventoryService = inventoryService;
        this.idempotentEventProcessor = idempotentEventProcessor;
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
            case CREATED -> handleOrderCreated(event);
            case CANCELLED -> handleOrderCancelled(event);
            default -> log.debug("Unhandled order event type: {}", event.getEventType());
        }
    }

    private void handleProductOrVariantCreated(ProductEvent event) {
        if (event.getVariantId() != null) {
            // Create inventory for variant
            Inventory inventory = new Inventory();
            inventory.setVariantId(event.getVariantId());
            inventory.setProductId(event.getProductId());
            inventory.setProductName(event.getProductName());
            inventory.setSkuCode(event.getSkuCode());
            inventory.setQuantity(0);
            inventory.setReservedQuantity(0);
            inventory.setReorderLevel(10);
            inventoryRepository.save(inventory);
            log.info("Created inventory for variant: {}", event.getVariantId());
        }
    }

    private void handleProductOrVariantUpdated(ProductEvent event) {
        if (event.getVariantId() != null) {
            inventoryRepository.findByVariantId(event.getVariantId())
                    .ifPresent(inventory -> {
                        inventory.setProductName(event.getProductName());
                        inventory.setSkuCode(event.getSkuCode());
                        inventoryRepository.save(inventory);
                        log.info("Updated inventory for variant: {}", event.getVariantId());
                    });
        }
    }

    private void handleProductOrVariantDeleted(ProductEvent event) {
        if (event.getVariantId() != null) {
            inventoryRepository.findByVariantId(event.getVariantId())
                    .ifPresent(inventoryRepository::delete);
            log.info("Deleted inventory for variant: {}", event.getVariantId());
        }
    }

    private void handleOrderCreated(OrderEvent event) {
        for (OrderEvent.OrderItem item : event.getItems()) {
            Long variantId = item.getVariantId();
            Integer quantity = item.getQuantity();
            Long orderItemId = item.getOrderItemId();

            if (variantId != null) {
                InventoryService.ReservationResult result = inventoryService.reserveStock(variantId, quantity, orderItemId);
                if (result.getBackordered() > 0) {
                    log.warn("Partial reservation for variant {}: reserved={}, backordered={}",
                            variantId, result.getReserved(), result.getBackordered());
                }
            }
        }
        log.info("Reserved stock for order: {}", event.getOrderId());
    }

    private void handleOrderCancelled(OrderEvent event) {
        for (OrderEvent.OrderItem item : event.getItems()) {
            Long variantId = item.getVariantId();
            Integer quantity = item.getQuantity();
            Long orderItemId = item.getOrderItemId();

            if (variantId != null) {
                inventoryService.releaseReservation(variantId, quantity, orderItemId);
            }
        }
        log.info("Released reservation for order: {}", event.getOrderId());
    }
}
