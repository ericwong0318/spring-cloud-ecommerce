package com.example.inventory.listener;

import com.example.common.event.BaseEvent;
import com.example.common.event.IdempotentEventProcessor;
import com.example.common.event.InventoryEvent;
import com.example.common.event.OrderEvent;
import com.example.common.event.ProductEvent;
import com.example.inventory.model.Inventory;
import com.example.inventory.repository.InventoryRepository;
import com.example.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventListener {

    private final InventoryRepository inventoryRepository;
    private final InventoryService inventoryService;
    private final IdempotentEventProcessor idempotentEventProcessor;

    @RabbitListener(queues = "${rabbitmq.queue.inventory-events}")
    @Transactional
    public void handleProductEvent(ProductEvent event) {
        idempotentEventProcessor.process(event, this::handleProductEventInternal);
    }

    private void handleProductEventInternal(ProductEvent event) {
        log.info("Received product event: {}", event);
        
        switch (ProductEvent.EventType.valueOf(event.getEventType())) {
            case CREATED -> handleProductCreated(event);
            case UPDATED -> handleProductUpdated(event);
            case DELETED -> handleProductDeleted(event);
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

    private void handleProductCreated(ProductEvent event) {
        Inventory inventory = new Inventory();
        inventory.setProductId(event.getProductId());
        inventory.setProductName(event.getProductName());
        inventory.setQuantity(0);
        inventory.setReservedQuantity(0);
        inventory.setReorderLevel(10);
        inventoryRepository.save(inventory);
        log.info("Created inventory for product: {}", event.getProductId());
    }

    private void handleProductUpdated(ProductEvent event) {
        inventoryRepository.findByProductId(event.getProductId())
                .ifPresent(inventory -> {
                    inventory.setProductName(event.getProductName());
                    inventoryRepository.save(inventory);
                    log.info("Updated inventory for product: {}", event.getProductId());
                });
    }

    private void handleProductDeleted(ProductEvent event) {
        inventoryRepository.findByProductId(event.getProductId())
                .ifPresent(inventoryRepository::delete);
        log.info("Deleted inventory for product: {}", event.getProductId());
    }

    private void handleOrderCreated(OrderEvent event) {
        for (OrderEvent.OrderItem item : event.getItems()) {
            BigDecimal price = item.getPrice();
            inventoryService.reserveStock(item.getProductId(), item.getQuantity());
        }
        log.info("Reserved stock for order: {}", event.getOrderId());
    }

    private void handleOrderCancelled(OrderEvent event) {
        for (OrderEvent.OrderItem item : event.getItems()) {
            inventoryService.releaseReservation(item.getProductId(), item.getQuantity());
        }
        log.info("Released reservation for order: {}", event.getOrderId());
    }
}