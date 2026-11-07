package com.example.inventory.service;

import com.example.common.event.InventoryEvent;
import com.example.common.event.OutboxEventPublisher;
import com.example.common.event.ReservationExpiredEvent;
import com.example.inventory.model.Inventory;
import com.example.inventory.model.Reservation;
import com.example.inventory.repository.InventoryRepository;
import com.example.inventory.repository.ReservationRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryRepository inventoryRepository;
    private final ReservationRepository reservationRepository;
    private final OutboxEventPublisher outboxPublisher;

    public InventoryService(InventoryRepository inventoryRepository,
                            ReservationRepository reservationRepository,
                            OutboxEventPublisher outboxPublisher) {
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
        this.outboxPublisher = outboxPublisher;
    }

    @CircuitBreaker(name = "inventory-service", fallbackMethod = "reserveStockFallback")
    @Retry(name = "inventory-service")
    @Transactional
    public Inventory.ReservationOutcome reserveStock(ReserveStockCommand command) {
        Optional<Inventory> inventoryOpt = inventoryRepository.findByVariantIdWithLock(command.variantId());
        if (inventoryOpt.isEmpty()) {
            log.warn("Inventory not found for variant: {}", command.variantId());
            Inventory inventory = new Inventory();
            inventory.setVariantId(command.variantId());
            inventory.setProductId(0L);
            inventory.setProductName("Unknown");
            inventory.setQuantity(0);
            inventory.setReservedQuantity(0);
            inventory.setReorderLevel(10);
            inventoryRepository.save(inventory);
            return new Inventory.ReservationOutcome(0, command.quantity());
        }

        Inventory inventory = inventoryOpt.get();
        Inventory.ReservationOutcome outcome = inventory.reserve(command.quantity());

        if (outcome.reserved() > 0) {
            inventoryRepository.save(inventory);
            log.info("Reserved {} units for variant {}", outcome.reserved(), command.variantId());
            createOrUpdateReservation(command.orderItemId(), command.variantId(), outcome.reserved());
            checkAndPublishLowStock(inventory);
        } else {
            log.warn("No stock available for variant {}: requested={}", command.variantId(), command.quantity());
        }
        publishInventoryEvent(InventoryEvent.reserved(
                command.variantId(), inventory.getProductId(),
                outcome.reserved(), outcome.backordered(),
                command.orderId(), command.customerId(), command.customerEmail()));
        return outcome;
    }

    private Inventory.ReservationOutcome reserveStockFallback(ReserveStockCommand command, Exception ex) {
        log.error("Circuit breaker fallback for reserveStock: variantId={}, error={}",
                command.variantId(), ex.getMessage());
        return new Inventory.ReservationOutcome(0, command.quantity());
    }

    @CircuitBreaker(name = "inventory-service", fallbackMethod = "releaseReservationFallback")
    @Retry(name = "inventory-service")
    @Transactional
    public void releaseReservation(Long variantId, Integer quantity, Long orderItemId) {
        Optional<Inventory> inventoryOpt = inventoryRepository.findByVariantIdWithLock(variantId);
        if (inventoryOpt.isPresent()) {
            Inventory inventory = inventoryOpt.get();
            inventory.release(quantity);
            inventoryRepository.save(inventory);
            log.info("Released {} units reservation for variant {}", quantity, variantId);
            publishInventoryEvent(InventoryEvent.released(variantId, inventory.getProductId(), quantity, inventory.getQuantity() - inventory.getReservedQuantity()));
            updateReservationStatus(orderItemId, Reservation.ReservationStatus.RELEASED);
            publishReservationExpiredEvent(orderItemId, variantId, quantity);
            checkAndPublishLowStock(inventory);
        }
    }

    private void releaseReservationFallback(Long variantId, Integer quantity, Long orderItemId, Exception ex) {
        log.error("Circuit breaker fallback for releaseReservation: variantId={}, error={}", variantId, ex.getMessage());
    }

    @CircuitBreaker(name = "inventory-service", fallbackMethod = "confirmStockFallback")
    @Retry(name = "inventory-service")
    @Transactional
    public void confirmStock(Long variantId, Integer quantity) {
        Optional<Inventory> inventoryOpt = inventoryRepository.findByVariantIdWithLock(variantId);
        if (inventoryOpt.isPresent()) {
            Inventory inventory = inventoryOpt.get();
            inventory.confirm(quantity);
            inventoryRepository.save(inventory);
            log.info("Confirmed stock reduction for variant {}: -{}", variantId, quantity);
            publishInventoryEvent(InventoryEvent.confirmed(variantId, inventory.getProductId(), quantity, inventory.getQuantity() - inventory.getReservedQuantity()));
            checkAndPublishLowStock(inventory);
        }
    }

    private void confirmStockFallback(Long variantId, Integer quantity, Exception ex) {
        log.error("Circuit breaker fallback for confirmStock: variantId={}, error={}", variantId, ex.getMessage());
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
            outboxPublisher.saveEvent("Inventory", variantId.toString(), "reservation.expired", event);
            log.debug("Saved ReservationExpiredEvent for OrderItem {} to outbox", orderItemId);
        }
    }

    private void checkAndPublishLowStock(Inventory inventory) {
        if (inventory.isLowStock() && !inventory.getLowStockNotified()) {
            inventory.setLowStockNotified(true);
            inventoryRepository.save(inventory);
            publishInventoryEvent(InventoryEvent.lowStock(inventory.getVariantId(), inventory.getProductId(),
                    inventory.getAvailableQuantity(), inventory.getReorderLevel()));
            log.warn("LOW_STOCK event published for variant {}: available={}, reorderLevel={}",
                    inventory.getVariantId(), inventory.getAvailableQuantity(), inventory.getReorderLevel());
        } else if (!inventory.isLowStock() && inventory.getLowStockNotified()) {
            inventory.setLowStockNotified(false);
            inventoryRepository.save(inventory);
            log.info("Stock replenished above reorder level for variant {}", inventory.getVariantId());
        }
    }

    private void publishInventoryEvent(InventoryEvent event) {
        outboxPublisher.saveEvent("Inventory", event.getVariantId().toString(), event.getEventType(), event);
        log.info("Saved InventoryEvent {} for variant {}", event.getEventType(), event.getVariantId());
    }

    public Optional<Inventory> getInventory(Long variantId) {
        return inventoryRepository.findByVariantId(variantId);
    }

    public static class InsufficientStockException extends RuntimeException {
        public InsufficientStockException(String message) {
            super(message);
        }
    }
}