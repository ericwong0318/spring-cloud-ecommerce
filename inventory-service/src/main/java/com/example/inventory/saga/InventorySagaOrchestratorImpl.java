package com.example.inventory.saga;

import com.example.common.event.OrderEvent;
import com.example.inventory.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class InventorySagaOrchestratorImpl implements InventorySagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(InventorySagaOrchestratorImpl.class);

    private final InventoryService inventoryService;

    public InventorySagaOrchestratorImpl(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Override
    public Mono<Void> handleOrderCreated(OrderEvent event) {
        log.info("Inventory saga orchestrator handling ORDER_CREATED for order: {}", event.getOrderId());
        for (OrderEvent.OrderItem item : event.getItems()) {
            Long variantId = item.getVariantId();
            Integer quantity = item.getQuantity();
            Long orderItemId = item.getOrderItemId();

            if (variantId != null) {
                InventoryService.ReservationResult result = inventoryService.reserveStock(
                        variantId, quantity, orderItemId, event.getOrderId(), event.getCustomerId(), event.getCustomerEmail());
                if (result.getBackordered() > 0) {
                    log.warn("Partial reservation for variant {}: reserved={}, backordered={}",
                            variantId, result.getReserved(), result.getBackordered());
                }
            }
        }
        log.info("Reserved stock for order: {}", event.getOrderId());
        return Mono.empty();
    }

    @Override
    public Mono<Void> handleOrderCancelled(OrderEvent event) {
        log.info("Inventory saga orchestrator handling ORDER_CANCELLED for order: {}", event.getOrderId());
        for (OrderEvent.OrderItem item : event.getItems()) {
            Long variantId = item.getVariantId();
            Integer quantity = item.getQuantity();
            Long orderItemId = item.getOrderItemId();

            if (variantId != null) {
                inventoryService.releaseReservation(variantId, quantity, orderItemId);
            }
        }
        log.info("Released reservation for order: {}", event.getOrderId());
        return Mono.empty();
    }
}