package com.example.inventory.scheduler;

import com.example.common.event.ReservationExpiredEvent;
import com.example.inventory.model.Inventory;
import com.example.inventory.model.Reservation;
import com.example.inventory.repository.InventoryRepository;
import com.example.inventory.repository.ReservationRepository;
import com.example.inventory.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class ReservationExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpiryScheduler.class);
    private static final long LOCK_KEY_OFFSET = 1000000L;

    private final ReservationRepository reservationRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryService inventoryService;
    private final RabbitTemplate rabbitTemplate;
    private final JdbcTemplate jdbcTemplate;

    @Value("${rabbitmq.exchange.inventory}")
    private String inventoryExchange;

    @Value("${rabbitmq.routing-key.reservation-expired}")
    private String reservationExpiredRoutingKey;

    public ReservationExpiryScheduler(ReservationRepository reservationRepository,
                                      InventoryRepository inventoryRepository,
                                      InventoryService inventoryService,
                                      RabbitTemplate rabbitTemplate,
                                      JdbcTemplate jdbcTemplate) {
        this.reservationRepository = reservationRepository;
        this.inventoryRepository = inventoryRepository;
        this.inventoryService = inventoryService;
        this.rabbitTemplate = rabbitTemplate;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void processExpiredReservations() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(15);
        List<Reservation> expiredReservations = reservationRepository.findExpiredReservations(cutoff);

        if (expiredReservations.isEmpty()) {
            log.debug("No expired reservations found");
            return;
        }

        log.info("Found {} expired reservations to process", expiredReservations.size());

        for (Reservation reservation : expiredReservations) {
            processExpiredReservation(reservation);
        }
    }

    private void processExpiredReservation(Reservation reservation) {
        Long orderItemId = reservation.getOrderItemId();
        Long variantId = reservation.getVariantId();
        Integer quantity = reservation.getQuantity();

        long lockKey = LOCK_KEY_OFFSET + orderItemId;
        boolean lockAcquired = tryAcquireAdvisoryLock(lockKey);

        if (!lockAcquired) {
            log.debug("Could not acquire lock for OrderItem {}, another instance may be processing it", orderItemId);
            return;
        }

        try {
            Optional<Inventory> inventoryOpt = inventoryRepository.findByVariantId(variantId);
            if (inventoryOpt.isPresent()) {
                Inventory inventory = inventoryOpt.get();
                int availableBefore = inventory.getQuantity() - inventory.getReservedQuantity();

                inventoryService.releaseReservation(variantId, quantity, orderItemId);

                Optional<Inventory> updatedOpt = inventoryRepository.findByVariantId(variantId);
                if (updatedOpt.isPresent()) {
                    Inventory updated = updatedOpt.get();
                    int availableAfter = updated.getQuantity() - updated.getReservedQuantity();

                    log.info("Released expired reservation for OrderItem {}: variantId={}, quantity={}, availableBefore={}, availableAfter={}",
                            orderItemId, variantId, quantity, availableBefore, availableAfter);
                }
            } else {
                log.warn("Inventory not found for variantId {} when processing expired OrderItem {}", variantId, orderItemId);
            }
        } finally {
            releaseAdvisoryLock(lockKey);
        }
    }

    private boolean tryAcquireAdvisoryLock(long lockKey) {
        try {
            Boolean result = jdbcTemplate.queryForObject(
                    "SELECT pg_try_advisory_lock(?)", Boolean.class, lockKey);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Error acquiring advisory lock for key {}: {}", lockKey, e.getMessage());
            return false;
        }
    }

    private void releaseAdvisoryLock(long lockKey) {
        try {
            jdbcTemplate.execute("SELECT pg_advisory_unlock(" + lockKey + ")");
        } catch (Exception e) {
            log.error("Error releasing advisory lock for key {}: {}", lockKey, e.getMessage());
        }
    }
}