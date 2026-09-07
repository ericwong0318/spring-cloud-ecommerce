package com.example.inventory.service;

import com.example.common.event.InventoryEvent;
import com.example.common.event.ReservationExpiredEvent;
import com.example.inventory.model.Inventory;
import com.example.inventory.repository.InventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryRepository inventoryRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.inventory}")
    private String inventoryExchange;

    @Value("${rabbitmq.routing-key.reservation-expired}")
    private String reservationExpiredRoutingKey;

    public InventoryService(InventoryRepository inventoryRepository,
                            RabbitTemplate rabbitTemplate) {
        this.inventoryRepository = inventoryRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    public ReservationResult reserveStock(Long variantId, Integer quantity) {
        Optional<Inventory> inventoryOpt = inventoryRepository.findByVariantId(variantId);
        if (inventoryOpt.isPresent()) {
            Inventory inventory = inventoryOpt.get();
            int available = inventory.getQuantity() - inventory.getReservedQuantity();

            if (available >= quantity) {
                inventory.setReservedQuantity(inventory.getReservedQuantity() + quantity);
                inventoryRepository.save(inventory);
                log.info("Reserved {} units for variant {}", quantity, variantId);
                publishInventoryEvent(InventoryEvent.reserved(variantId, inventory.getProductId(), quantity, 0));
                return new ReservationResult(quantity, 0);
            } else if (available > 0) {
                int backordered = quantity - available;
                inventory.setReservedQuantity(inventory.getReservedQuantity() + available);
                inventoryRepository.save(inventory);
                log.info("Partially reserved {} units for variant {}, backordered {}", available, variantId, backordered);
                publishInventoryEvent(InventoryEvent.reserved(variantId, inventory.getProductId(), available, backordered));
                return new ReservationResult(available, backordered);
            } else {
                log.warn("No stock available for variant {}: requested={}", variantId, quantity);
                publishInventoryEvent(InventoryEvent.reserved(variantId, inventory.getProductId(), 0, quantity));
                return new ReservationResult(0, quantity);
            }
        } else {
            log.warn("Inventory not found for variant: {}", variantId);
            Inventory inventory = new Inventory();
            inventory.setVariantId(variantId);
            inventory.setProductId(0L);
            inventory.setProductName("Unknown");
            inventory.setQuantity(0);
            inventory.setReservedQuantity(0);
            inventory.setReorderLevel(10);
            inventoryRepository.save(inventory);
            return new ReservationResult(0, quantity);
        }
    }

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
                publishInventoryEvent(InventoryEvent.reserved(null, productId, quantity, 0));
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
            publishInventoryEvent(InventoryEvent.released(variantId, inventory.getProductId(), quantity, inventory.getQuantity() - newReserved));
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
            publishInventoryEvent(InventoryEvent.released(null, productId, quantity, inventory.getQuantity() - newReserved));
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
            publishInventoryEvent(InventoryEvent.confirmed(variantId, inventory.getProductId(), quantity, inventory.getQuantity() - inventory.getReservedQuantity()));
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
            publishInventoryEvent(InventoryEvent.confirmed(null, productId, quantity, inventory.getQuantity() - inventory.getReservedQuantity()));
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
            publishInventoryEvent(InventoryEvent.stockAdded(variantId, inventory.getProductId(), quantity, inventory.getQuantity() - inventory.getReservedQuantity()));
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
            publishInventoryEvent(InventoryEvent.stockAdded(null, productId, quantity, inventory.getQuantity() - inventory.getReservedQuantity()));
            checkLowStock(inventory);
        }
    }

    private void checkLowStock(Inventory inventory) {
        int available = inventory.getQuantity() - inventory.getReservedQuantity();
        if (available <= inventory.getReorderLevel()) {
            publishInventoryEvent(InventoryEvent.lowStock(inventory.getVariantId(), inventory.getProductId(), available, inventory.getReorderLevel()));
        }
    }

    private void publishInventoryEvent(InventoryEvent event) {
        rabbitTemplate.convertAndSend(inventoryExchange, event.getEventType().toLowerCase(), event);
        log.info("Published InventoryEvent {} for variant {}", event.getEventType(), event.getVariantId());
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

    @Scheduled(fixedDelay = 60000)
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
                publishInventoryEvent(InventoryEvent.released(inventory.getVariantId(), inventory.getProductId(), releasedQuantity, inventory.getQuantity()));
                publishReservationExpiredEvent(inventory.getVariantId(), releasedQuantity, threshold);
            }
        }
    }

    private void publishReservationExpiredEvent(Long variantId, Integer quantityReleased, LocalDateTime reservationExpiresAt) {
        ReservationExpiredEvent event = ReservationExpiredEvent.expired(
                null, variantId, quantityReleased, reservationExpiresAt
        );
        rabbitTemplate.convertAndSend(inventoryExchange, reservationExpiredRoutingKey, event);
        log.info("Published ReservationExpiredEvent for variant {}: quantityReleased={}", variantId, quantityReleased);
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