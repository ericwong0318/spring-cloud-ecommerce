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

import java.math.BigDecimal;

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
    public void handleProductEvent(ProductEvent event) {
        idempotentEventProcessor.process(event, this::handleProductEventInternal);
    }

    private void handleProductEventInternal(ProductEvent event) {
        log.info("Received product event: {}", event);
        
        switch (ProductEvent.EventType.valueOf(event.getEventType())) {
            case CREATED, VARIANT_CREATED -> handleProductOrVariantCreated(event);
            case UPDATED, VARIANT_UPDATED -> handleProductOrVariantUpdated(event);
            case DELETED, VARIANT_DELETED -> handleProductOrVariantDeleted(event);
        }
    }

    @RabbitListener(queues = "${rabbitmq.queue.inventory-events}")
    @Transactional
    public void handleOrderEvent(OrderEvent event) {
        idempotentEventProcessor.process(event, this::handleOrderEventInternal);
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
        } else if (event.getProductId() != null) {
            // Legacy product creation (backward compatibility)
            Inventory inventory = new Inventory();
            inventory.setProductId(event.getProductId());
            inventory.setProductName(event.getProductName());
            inventory.setQuantity(0);
            inventory.setReservedQuantity(0);
            inventory.setReorderLevel(10);
            inventoryRepository.save(inventory);
            log.info("Created inventory for product: {}", event.getProductId());
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
        } else if (event.getProductId() != null) {
            inventoryRepository.findByProductId(event.getProductId())
                    .ifPresent(inventory -> {
                        inventory.setProductName(event.getProductName());
                        inventoryRepository.save(inventory);
                        log.info("Updated inventory for product: {}", event.getProductId());
                    });
        }
    }

    private void handleProductOrVariantDeleted(ProductEvent event) {
        if (event.getVariantId() != null) {
            inventoryRepository.findByVariantId(event.getVariantId())
                    .ifPresent(inventoryRepository::delete);
            log.info("Deleted inventory for variant: {}", event.getVariantId());
        } else if (event.getProductId() != null) {
            inventoryRepository.findByProductId(event.getProductId())
                    .ifPresent(inventoryRepository::delete);
            log.info("Deleted inventory for product: {}", event.getProductId());
        }
    }

    private void handleOrderCreated(OrderEvent event) {
        for (OrderEvent.OrderItem item : event.getItems()) {
            Long variantId = item.getVariantId();
            Long productId = item.getProductId();
            Integer quantity = item.getQuantity();
            
            if (variantId != null) {
                InventoryService.ReservationResult result = inventoryService.reserveStock(variantId, quantity);
                if (result.getBackorderedQuantity() > 0) {
                    log.warn("Partial reservation for variant {}: reserved={}, backordered={}", 
                            variantId, result.getReservedQuantity(), result.getBackorderedQuantity());
                }
            } else if (productId != null) {
                // Legacy productId-based reservation
                inventoryService.reserveStockByProductId(productId, quantity);
            }
        }
        log.info("Reserved stock for order: {}", event.getOrderId());
    }

    private void handleOrderCancelled(OrderEvent event) {
        for (OrderEvent.OrderItem item : event.getItems()) {
            Long variantId = item.getVariantId();
            Long productId = item.getProductId();
            Integer quantity = item.getQuantity();
            
            if (variantId != null) {
                inventoryService.releaseReservation(variantId, quantity);
            } else if (productId != null) {
                inventoryService.releaseReservationByProductId(productId, quantity);
            }
        }
        log.info("Released reservation for order: {}", event.getOrderId());
    }
}