package com.example.inventory.service;

import com.example.common.event.InventoryEvent;
import com.example.common.event.ReservationExpiredEvent;
import com.example.inventory.model.Inventory;
import com.example.inventory.model.Reservation;
import com.example.inventory.repository.InventoryRepository;
import com.example.inventory.repository.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryRepository inventoryRepository;
    private final ReservationRepository reservationRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.inventory}")
    private String inventoryExchange;

    @Value("${rabbitmq.routing-key.reservation-expired}")
    private String reservationExpiredRoutingKey;

    public InventoryService(InventoryRepository inventoryRepository,
                            ReservationRepository reservationRepository,
                            RabbitTemplate rabbitTemplate) {
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    public ReservationResult reserveStock(Long variantId, Integer quantity, Long orderItemId) {
        Optional<Inventory> inventoryOpt = inventoryRepository.findByVariantIdWithLock(variantId);
        if (inventoryOpt.isPresent()) {
            Inventory inventory = inventoryOpt.get();
            int available = inventory.getQuantity() - inventory.getReservedQuantity();

            if (available >= quantity) {
                inventory.setReservedQuantity(inventory.getReservedQuantity() + quantity);
                inventoryRepository.save(inventory);
                log.info("Reserved {} units for variant {}", quantity, variantId);
                publishInventoryEvent(InventoryEvent.reserved(variantId, inventory.getProductId(), quantity, 0));
                createOrUpdateReservation(orderItemId, variantId, quantity);
                checkAndPublishLowStock(inventory);
                return new ReservationResult(quantity, 0);
            } else if (available > 0) {
                int backordered = quantity - available;
                inventory.setReservedQuantity(inventory.getReservedQuantity() + available);
                inventoryRepository.save(inventory);
                log.info("Partially reserved {} units for variant {}, backordered {}", available, variantId, backordered);
                publishInventoryEvent(InventoryEvent.reserved(variantId, inventory.getProductId(), available, backordered));
                createOrUpdateReservation(orderItemId, variantId, available);
                checkAndPublishLowStock(inventory);
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
    public void releaseReservation(Long variantId, Integer quantity, Long orderItemId) {
        Optional<Inventory> inventoryOpt = inventoryRepository.findByVariantIdWithLock(variantId);
        if (inventoryOpt.isPresent()) {
            Inventory inventory = inventoryOpt.get();
            int newReserved = Math.max(0, inventory.getReservedQuantity() - quantity);
            inventory.setReservedQuantity(newReserved);
            inventoryRepository.save(inventory);
            log.info("Released {} units reservation for variant {}", quantity, variantId);
            publishInventoryEvent(InventoryEvent.released(variantId, inventory.getProductId(), quantity, inventory.getQuantity() - newReserved));
            updateReservationStatus(orderItemId, Reservation.ReservationStatus.RELEASED);
            publishReservationExpiredEvent(orderItemId, variantId, quantity);
            checkAndPublishLowStock(inventory);
        }
    }

    @Transactional
    public void confirmStock(Long variantId, Integer quantity) {
        Optional<Inventory> inventoryOpt = inventoryRepository.findByVariantIdWithLock(variantId);
        if (inventoryOpt.isPresent()) {
            Inventory inventory = inventoryOpt.get();
            inventory.setQuantity(inventory.getQuantity() - quantity);
            inventory.setReservedQuantity(inventory.getReservedQuantity() - quantity);
            inventoryRepository.save(inventory);
            log.info("Confirmed stock reduction for variant {}: -{}", variantId, quantity);
            publishInventoryEvent(InventoryEvent.confirmed(variantId, inventory.getProductId(), quantity, inventory.getQuantity() - inventory.getReservedQuantity()));
            checkAndPublishLowStock(inventory);
        }
    }

    private void createOrUpdateReservation(Long orderItemId, Long variantId, Integer quantity) {
        if (orderItemId != null) {
            Optional<Reservation> existing = reservationRepository.findByOrderItemId(orderItemId);
            if (existing.isPresent()) {
                Reservation reservation = existing.get();
                reservation.setQuantity(reservation.getQuantity() + quantity);
                reservation.setReservedAt(LocalDateTime.now());
                reservation.setStatus(Reservation.ReservationStatus.RESERVED);
                reservationRepository.save(reservation);
            } else {
                Reservation reservation = new Reservation(orderItemId, variantId, quantity, LocalDateTime.now());
                reservationRepository.save(reservation);
            }
        }
    }

    private void updateReservationStatus(Long orderItemId, Reservation.ReservationStatus status) {
        if (orderItemId != null) {
            Optional<Reservation> existing = reservationRepository.findByOrderItemId(orderItemId);
            if (existing.isPresent()) {
                Reservation reservation = existing.get();
                reservation.setStatus(status);
                reservation.setReleasedAt(LocalDateTime.now());
                reservationRepository.save(reservation);
            }
        }
    }

    private void publishReservationExpiredEvent(Long orderItemId, Long variantId, Integer quantityReleased) {
        if (orderItemId != null) {
            ReservationExpiredEvent event = ReservationExpiredEvent.expired(orderItemId, variantId, quantityReleased, LocalDateTime.now());
            try {
                rabbitTemplate.convertAndSend(inventoryExchange, reservationExpiredRoutingKey, event);
                log.debug("Published ReservationExpiredEvent for OrderItem {}", orderItemId);
            } catch (Exception e) {
                log.error("Failed to publish ReservationExpiredEvent for OrderItem {}: {}", orderItemId, e.getMessage());
            }
        }
    }

    private void checkAndPublishLowStock(Inventory inventory) {
        int available = inventory.getAvailableQuantity();
        if (available <= inventory.getReorderLevel() && !inventory.getLowStockNotified()) {
            inventory.setLowStockNotified(true);
            inventoryRepository.save(inventory);
            publishInventoryEvent(InventoryEvent.lowStock(inventory.getVariantId(), inventory.getProductId(), available, inventory.getReorderLevel()));
            log.warn("LOW_STOCK event published for variant {}: available={}, reorderLevel={}",
                    inventory.getVariantId(), available, inventory.getReorderLevel());
        } else if (available > inventory.getReorderLevel() && inventory.getLowStockNotified()) {
            inventory.setLowStockNotified(false);
            inventoryRepository.save(inventory);
            log.info("Stock replenished above reorder level for variant {}", inventory.getVariantId());
        }
    }

    private void publishInventoryEvent(InventoryEvent event) {
        CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());
        correlationData.getFuture().whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish InventoryEvent {} for variant {}: {}", event.getEventType(), event.getVariantId(), ex.getMessage());
            } else if (result != null && result.isAck()) {
                log.debug("InventoryEvent {} for variant {} confirmed by broker", event.getEventType(), event.getVariantId());
            } else {
                log.warn("InventoryEvent {} for variant {} not acknowledged by broker", event.getEventType(), event.getVariantId());
            }
        });
        rabbitTemplate.convertAndSend(inventoryExchange, event.getEventType().toLowerCase(), event, correlationData);
        log.info("Published InventoryEvent {} for variant {}", event.getEventType(), event.getVariantId());
    }

    public Optional<Inventory> getInventory(Long variantId) {
        return inventoryRepository.findByVariantId(variantId);
    }

    public static class InsufficientStockException extends RuntimeException {
        public InsufficientStockException(String message) {
            super(message);
        }
    }

    public static class ReservationResult {
        private final int reserved;
        private final int backordered;

        public ReservationResult(int reserved, int backordered) {
            this.reserved = reserved;
            this.backordered = backordered;
        }

        public int getReserved() {
            return reserved;
        }

        public int getBackordered() {
            return backordered;
        }

        public boolean isFullyReserved() {
            return backordered == 0;
        }

        public boolean isPartiallyReserved() {
            return reserved > 0 && backordered > 0;
        }

        public boolean isFullyBackordered() {
            return reserved == 0 && backordered > 0;
        }
    }
}