package com.example.inventory.service;

import com.example.common.event.OutboxEventPublisher;
import com.example.common.event.InventoryEvent;
import com.example.inventory.model.Inventory;
import com.example.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final OutboxEventPublisher outboxEventPublisher;

    /**
     * Reserve stock for a variant. Supports partial reservation with backorder.
     * 
     * @param variantId The variant ID
     * @param quantity The quantity to reserve
     * @return ReservationResult containing reserved and backordered quantities
     */
    @Transactional
    public ReservationResult reserveStock(Long variantId, Integer quantity) {
        Optional<Inventory> inventoryOpt = inventoryRepository.findByVariantId(variantId);
        if (inventoryOpt.isPresent()) {
            Inventory inventory = inventoryOpt.get();
            int available = inventory.getQuantity() - inventory.getReservedQuantity();
            
            if (available >= quantity) {
                // Full reservation possible
                inventory.setReservedQuantity(inventory.getReservedQuantity() + quantity);
                inventoryRepository.save(inventory);
                log.info("Reserved {} units for variant {}", quantity, variantId);
                publishInventoryEvent(InventoryEvent.reserved(variantId, inventory.getReservedQuantity(), available - quantity));
                return new ReservationResult(quantity, 0);
            } else if (available > 0) {
                // Partial reservation - reserve what's available, backorder the rest
                int backordered = quantity - available;
                inventory.setReservedQuantity(inventory.getReservedQuantity() + available);
                inventoryRepository.save(inventory);
                log.info("Partially reserved {} units for variant {}, backordered {}", available, variantId, backordered);
                publishInventoryEvent(InventoryEvent.reserved(variantId, inventory.getReservedQuantity(), 0));
                return new ReservationResult(available, backordered);
            } else {
                // No stock available - full backorder
                log.warn("No stock available for variant {}: requested={}", variantId, quantity);
                publishInventoryEvent(InventoryEvent.reserved(variantId, inventory.getReservedQuantity(), 0));
                return new ReservationResult(0, quantity);
            }
        } else {
            log.warn("Inventory not found for variant: {}", variantId);
            // Create inventory record for this variant (eventual consistency)
            Inventory inventory = new Inventory();
            inventory.setVariantId(variantId);
            inventory.setProductId(0L); // Will be updated when ProductEvent arrives
            inventory.setProductName("Unknown");
            inventory.setQuantity(0);
            inventory.setReservedQuantity(0);
            inventory.setReorderLevel(10);
            inventoryRepository.save(inventory);
            return new ReservationResult(0, quantity);
        }
    }

    /**
     * Reserve stock by productId (legacy method for backward compatibility)
     */
    @Transactional
    public void reserveStockByProductId(Long productId, Integer quantity) {
        Optional<Inventory> inventoryOpt = inventoryRepository.findByProductId(productId);
        if (inventoryOpt.isPresent()) {
            Inventory inventory = inventoryOpt.get();
            int available = inventory.getQuantity() - inventory.getReservedQuantity();
            if (available >= quantity) {
                inventory.setReservedQuantity(inventory.getReservedQuantity() + quantity);
                inventoryRepository.save(inventory);
                log.info("Reserved {} units for product {}", quantity, productId);
                publishInventoryEvent(InventoryEvent.reserved(productId, inventory.getReservedQuantity(), available - quantity));
            } else {
                log.warn("Insufficient stock for product {}: available={}, requested={}", 
                        productId, available, quantity);
                throw new InsufficientStockException(
                        "Insufficient stock for product " + productId + 
                        ": available=" + available + ", requested=" + quantity);
            }
        } else {
            log.warn("Inventory not found for product: {}", productId);
        }
    }

    @Transactional
    public void releaseReservation(Long variantId, Integer quantity) {
        Optional<Inventory> inventoryOpt = inventoryRepository.findByVariantId(variantId);
        if (inventoryOpt.isPresent()) {
            Inventory inventory = inventoryOpt.get();
            int newReserved = Math.max(0, inventory.getReservedQuantity() - quantity);
            inventory.setReservedQuantity(newReserved);
            inventoryRepository.save(inventory);
            log.info("Released {} units reservation for variant {}", quantity, variantId);
            publishInventoryEvent(InventoryEvent.released(variantId, inventory.getReservedQuantity(), inventory.getQuantity() - inventory.getReservedQuantity()));
        }
    }

    @Transactional
    public void releaseReservationByProductId(Long productId, Integer quantity) {
        Optional<Inventory> inventoryOpt = inventoryRepository.findByProductId(productId);
        if (inventoryOpt.isPresent()) {
            Inventory inventory = inventoryOpt.get();
            int newReserved = Math.max(0, inventory.getReservedQuantity() - quantity);
            inventory.setReservedQuantity(newReserved);
            inventoryRepository.save(inventory);
            log.info("Released {} units reservation for product {}", quantity, productId);
            publishInventoryEvent(InventoryEvent.released(productId, inventory.getReservedQuantity(), inventory.getQuantity() - inventory.getReservedQuantity()));
        }
    }

    @Transactional
    public void confirmStock(Long variantId, Integer quantity) {
        Optional<Inventory> inventoryOpt = inventoryRepository.findByVariantId(variantId);
        if (inventoryOpt.isPresent()) {
            Inventory inventory = inventoryOpt.get();
            inventory.setQuantity(inventory.getQuantity() - quantity);
            inventory.setReservedQuantity(inventory.getReservedQuantity() - quantity);
            inventoryRepository.save(inventory);
            log.info("Confirmed stock reduction for variant {}: -{}", variantId, quantity);
            publishInventoryEvent(InventoryEvent.confirmed(variantId, inventory.getQuantity(), inventory.getQuantity() - inventory.getReservedQuantity()));
            checkLowStock(inventory);
        }
    }

    @Transactional
    public void confirmStockByProductId(Long productId, Integer quantity) {
        Optional<Inventory> inventoryOpt = inventoryRepository.findByProductId(productId);
        if (inventoryOpt.isPresent()) {
            Inventory inventory = inventoryOpt.get();
            inventory.setQuantity(inventory.getQuantity() - quantity);
            inventory.setReservedQuantity(inventory.getReservedQuantity() - quantity);
            inventoryRepository.save(inventory);
            log.info("Confirmed stock reduction for product {}: -{}", productId, quantity);
            publishInventoryEvent(InventoryEvent.confirmed(productId, inventory.getQuantity(), inventory.getQuantity() - inventory.getReservedQuantity()));
            checkLowStock(inventory);
        }
    }

    @Transactional
    public void addStock(Long variantId, Integer quantity) {
        Optional<Inventory> inventoryOpt = inventoryRepository.findByVariantId(variantId);
        if (inventoryOpt.isPresent()) {
            Inventory inventory = inventoryOpt.get();
            inventory.setQuantity(inventory.getQuantity() + quantity);
            inventoryRepository.save(inventory);
            log.info("Added {} units to variant {}", quantity, variantId);
            publishInventoryEvent(InventoryEvent.stockAdded(variantId, inventory.getQuantity(), inventory.getQuantity() - inventory.getReservedQuantity()));
            checkLowStock(inventory);
        }
    }

    @Transactional
    public void addStockByProductId(Long productId, Integer quantity) {
        Optional<Inventory> inventoryOpt = inventoryRepository.findByProductId(productId);
        if (inventoryOpt.isPresent()) {
            Inventory inventory = inventoryOpt.get();
            inventory.setQuantity(inventory.getQuantity() + quantity);
            inventoryRepository.save(inventory);
            log.info("Added {} units to product {}", quantity, productId);
            publishInventoryEvent(InventoryEvent.stockAdded(productId, inventory.getQuantity(), inventory.getQuantity() - inventory.getReservedQuantity()));
            checkLowStock(inventory);
        }
    }

    private void checkLowStock(Inventory inventory) {
        int available = inventory.getQuantity() - inventory.getReservedQuantity();
        if (available <= inventory.getReorderLevel()) {
            publishInventoryEvent(InventoryEvent.lowStock(inventory.getVariantId(), available, inventory.getReorderLevel()));
        }
    }

    private void publishInventoryEvent(InventoryEvent event) {
        outboxEventPublisher.saveEvent("Inventory", event.getProductId().toString(), event.getEventType(), event);
    }

    public Optional<Inventory> getInventory(Long variantId) {
        return inventoryRepository.findByVariantId(variantId);
    }

    public Optional<Inventory> getInventoryByProductId(Long productId) {
        return inventoryRepository.findByProductId(productId);
    }

    public List<Inventory> getLowStockItems() {
        return inventoryRepository.findByQuantityLessThanEqualReorderLevel();
    }

    /**
     * Scheduled task to release stale reservations older than 15 minutes.
     * Runs every minute.
     */
    @Scheduled(fixedDelay = 60000) // 1 minute
    @Transactional
    public void releaseStaleReservations() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(15);
        List<Inventory> staleReservations = inventoryRepository.findStaleReservations(threshold);
        
        for (Inventory inventory : staleReservations) {
            int releasedQuantity = inventory.getReservedQuantity();
            if (releasedQuantity > 0) {
                inventory.setReservedQuantity(0);
                inventoryRepository.save(inventory);
                log.info("Released stale reservation for variant {}: {} units", inventory.getVariantId(), releasedQuantity);
                publishInventoryEvent(InventoryEvent.released(inventory.getVariantId(), 0, inventory.getQuantity()));
            }
        }
    }

    public static class InsufficientStockException extends RuntimeException {
        public InsufficientStockException(String message) {
            super(message);
        }
    }

    public static class ReservationResult {
        private final int reservedQuantity;
        private final int backorderedQuantity;

        public ReservationResult(int reservedQuantity, int backorderedQuantity) {
            this.reservedQuantity = reservedQuantity;
            this.backorderedQuantity = backorderedQuantity;
        }

        public int getReservedQuantity() {
            return reservedQuantity;
        }

        public int getBackorderedQuantity() {
            return backorderedQuantity;
        }

        public boolean isFullyReserved() {
            return backorderedQuantity == 0;
        }

        public boolean isPartiallyReserved() {
            return reservedQuantity > 0 && backorderedQuantity > 0;
        }

        public boolean isFullyBackordered() {
            return reservedQuantity == 0 && backorderedQuantity > 0;
        }
    }
}