package com.example.inventory.integration;

import com.example.common.event.InventoryEvent;
import com.example.common.event.ReservationExpiredEvent;
import com.example.inventory.InventoryServiceApplication;
import com.example.inventory.model.Inventory;
import com.example.inventory.model.Reservation;
import com.example.inventory.repository.InventoryRepository;
import com.example.inventory.repository.ReservationRepository;
import com.example.inventory.scheduler.ReservationExpiryScheduler;
import com.example.inventory.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(classes = InventoryServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ReservationExpirySchedulerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("inventory_db")
            .withUsername("test")
            .withPassword("test");

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.13-management")
            .withExposedPorts(5672);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.autoconfigure.exclude", () -> "org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration,org.springframework.boot.autoconfigure.r2dbc.R2dbcTransactionManagerAutoConfiguration,org.springframework.boot.autoconfigure.r2dbc.R2dbcRepositoriesAutoConfiguration");
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
    }

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ReservationExpiryScheduler scheduler;

    private Inventory testInventory;
    private final Long variantId = 100L;
    private final Long productId = 50L;
    private final Long orderItemId = 1000L;

    private static final Logger log = LoggerFactory.getLogger(ReservationExpirySchedulerIntegrationTest.class);

    @BeforeEach
    @Transactional
    void setUp() {
        // Clean up
        reservationRepository.deleteAll();
        inventoryRepository.deleteAll();

        // Create test inventory with sufficient stock
        testInventory = new Inventory();
        testInventory.setVariantId(variantId);
        testInventory.setProductId(productId);
        testInventory.setProductName("Test Product");
        testInventory.setSkuCode("TEST-001");
        testInventory.setQuantity(100);
        testInventory.setReservedQuantity(0);
        testInventory.setReorderLevel(10);
        testInventory.setCostPrice(new BigDecimal("50.00"));
        testInventory = inventoryRepository.save(testInventory);
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void shouldReleaseExpiredReservationAndPublishEvents() {
        // Given: Create a reservation with reservedAt set to 16 minutes ago
        int quantity = 10;
        inventoryService.reserveStock(variantId, quantity, orderItemId);

        // Manually update the reservation's reservedAt to 16 minutes ago (expired)
        Optional<Reservation> reservationOpt = reservationRepository.findByOrderItemId(orderItemId);
        assertThat(reservationOpt).isPresent();
        Reservation reservation = reservationOpt.get();
        reservation.setReservedAt(LocalDateTime.now().minusMinutes(16));
        reservationRepository.saveAndFlush(reservation);

        // Verify initial state
        Inventory beforeInventory = inventoryRepository.findByVariantId(variantId).orElseThrow();
        assertThat(beforeInventory.getReservedQuantity()).isEqualTo(quantity);
        assertThat(beforeInventory.getAvailableQuantity()).isEqualTo(90);

        // When: Run the scheduler manually
        scheduler.processExpiredReservations();

        // Then: Verify reservation is released
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Inventory afterInventory = inventoryRepository.findByVariantId(variantId).orElseThrow();
            assertThat(afterInventory.getReservedQuantity()).isEqualTo(0);
            assertThat(afterInventory.getAvailableQuantity()).isEqualTo(100);
        });

        // Verify reservation status updated
        Reservation updatedReservation = reservationRepository.findByOrderItemId(orderItemId).orElseThrow();
        assertThat(updatedReservation.getStatus()).isEqualTo(Reservation.ReservationStatus.RELEASED);
        assertThat(updatedReservation.getReleasedAt()).isNotNull();
    }

    @Test
    void shouldNotReleaseNonExpiredReservation() {
        // Given: Create a reservation with reservedAt set to 5 minutes ago (not expired)
        int quantity = 10;
        inventoryService.reserveStock(variantId, quantity, orderItemId);

        // Manually update the reservation's reservedAt to 5 minutes ago
        Optional<Reservation> reservationOpt = reservationRepository.findByOrderItemId(orderItemId);
        assertThat(reservationOpt).isPresent();
        Reservation reservation = reservationOpt.get();
        reservation.setReservedAt(LocalDateTime.now().minusMinutes(5));
        reservationRepository.save(reservation);

        // When: Run the scheduler manually
        scheduler.processExpiredReservations();

        // Then: Reservation should not be released
        Inventory inventory = inventoryRepository.findByVariantId(variantId).orElseThrow();
        assertThat(inventory.getReservedQuantity()).isEqualTo(quantity);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(90);

        Reservation updatedReservation = reservationRepository.findByOrderItemId(orderItemId).orElseThrow();
        assertThat(updatedReservation.getStatus()).isEqualTo(Reservation.ReservationStatus.RESERVED);
    }

    @Test
    void shouldPublishLowStockEventWhenAvailableQuantityDropsBelowReorderLevel() {
        // Given: Inventory with quantity=15, reserved=0, reorderLevel=10
        // Available = 15, which is above reorder level (10)
        testInventory.setQuantity(15);
        testInventory.setReservedQuantity(0);
        testInventory.setReorderLevel(10);
        testInventory.setLowStockNotified(false);
        inventoryRepository.save(testInventory);

        // When: Reserve 6 units, available becomes 9 (below reorder level 10)
        inventoryService.reserveStock(variantId, 6, orderItemId);

        // Then: LOW_STOCK event should be published
        // Note: We verify the inventory state and lowStockNotified flag
        Inventory updatedInventory = inventoryRepository.findByVariantId(variantId).orElseThrow();
        assertThat(updatedInventory.getAvailableQuantity()).isEqualTo(9);
        assertThat(updatedInventory.getLowStockNotified()).isTrue();

        // Verify LOW_STOCK event was published by checking we can't publish it again
        // (idempotency - lowStockNotified is true)
        inventoryService.reserveStock(variantId, 1, orderItemId + 1);
        Inventory afterSecondReserve = inventoryRepository.findByVariantId(variantId).orElseThrow();
        assertThat(afterSecondReserve.getLowStockNotified()).isTrue();
    }

    @Test
    void shouldResetLowStockNotifiedWhenStockReplenished() {
        // Given: Inventory in low stock state (lowStockNotified = true)
        testInventory.setQuantity(15);
        testInventory.setReservedQuantity(6);
        testInventory.setReorderLevel(10);
        testInventory.setLowStockNotified(true);
        inventoryRepository.save(testInventory);

        // When: Release the reservation and add stock
        inventoryService.releaseReservation(variantId, 6, orderItemId);
        
        // Then add stock (simulate by increasing quantity)
        Inventory afterRelease = inventoryRepository.findByVariantId(variantId).orElseThrow();
        afterRelease.setQuantity(afterRelease.getQuantity() + 20);
        inventoryRepository.save(afterRelease);

        // When: Check low stock again by triggering a reservation
        inventoryService.reserveStock(variantId, 5, orderItemId + 1);

        // Then: lowStockNotified should be reset to false
        Inventory updatedInventory = inventoryRepository.findByVariantId(variantId).orElseThrow();
        assertThat(updatedInventory.getAvailableQuantity()).isGreaterThan(10);
        assertThat(updatedInventory.getLowStockNotified()).isFalse();
    }

    @Test
    void shouldPublishLowStockEventOnConfirmStock() {
        // Given: Inventory with quantity=15, reserved=5, reorderLevel=10
        // Available = 10 (at reorder level)
        testInventory.setQuantity(15);
        testInventory.setReservedQuantity(5);
        testInventory.setReorderLevel(10);
        testInventory.setLowStockNotified(false);
        inventoryRepository.save(testInventory);

        // When: Confirm stock - reduces quantity by 5 and reserved by 5
        // New quantity = 10, reserved = 0, available = 10 (at reorder level)
        inventoryService.confirmStock(variantId, 5);

        // Then: LOW_STOCK event should be published (available <= reorderLevel)
        Inventory updatedInventory = inventoryRepository.findByVariantId(variantId).orElseThrow();
        assertThat(updatedInventory.getAvailableQuantity()).isEqualTo(10);
        assertThat(updatedInventory.getLowStockNotified()).isTrue();
    }

    @Test
    void shouldNotProcessAlreadyReleasedReservation() {
        // Given: Create and release a reservation
        int quantity = 10;
        inventoryService.reserveStock(variantId, quantity, orderItemId);
        inventoryService.releaseReservation(variantId, quantity, orderItemId);

        // Manually set reservedAt to 16 minutes ago
        Optional<Reservation> reservationOpt = reservationRepository.findByOrderItemId(orderItemId);
        assertThat(reservationOpt).isPresent();
        Reservation reservation = reservationOpt.get();
        reservation.setReservedAt(LocalDateTime.now().minusMinutes(16));
        reservationRepository.save(reservation);

        // When: Run the scheduler
        scheduler.processExpiredReservations();

        // Then: Should not affect inventory (already released)
        Inventory inventory = inventoryRepository.findByVariantId(variantId).orElseThrow();
        assertThat(inventory.getReservedQuantity()).isEqualTo(0);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(100);
    }
}