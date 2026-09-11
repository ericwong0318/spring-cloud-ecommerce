package com.example.inventory.integration;

import com.example.common.dto.InventoryDto;
import com.example.common.event.InventoryEvent;
import com.example.common.event.ReservationExpiredEvent;
import com.example.inventory.InventoryServiceApplication;
import com.example.inventory.model.Inventory;
import com.example.inventory.model.Reservation;
import com.example.inventory.repository.InventoryRepository;
import com.example.inventory.repository.ReservationRepository;
import com.example.inventory.service.InventoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = InventoryServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class InventoryControllerIntegrationTest {

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
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private Long variantId = 100L;
    private Long productId = 50L;
    private Long orderItemId = 1000L;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        inventoryRepository.deleteAll();

        Inventory testInventory = new Inventory();
        testInventory.setVariantId(variantId);
        testInventory.setProductId(productId);
        testInventory.setProductName("Test Product");
        testInventory.setSkuCode("TEST-001");
        testInventory.setQuantity(100);
        testInventory.setReservedQuantity(0);
        testInventory.setReorderLevel(10);
        testInventory.setCostPrice(new BigDecimal("50.00"));
        inventoryRepository.save(testInventory);
    }

    @Test
    void shouldReserveStockViaController() throws Exception {
        // Given
        String requestBody = """
            {
                "variantId": 100,
                "quantity": 10,
                "orderItemId": 1000
            }
            """;

        // When
        mockMvc.perform(post("/inventory/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variantId").value(100))
                .andExpect(jsonPath("$.quantity").value(100))
                .andExpect(jsonPath("$.reservedQuantity").value(10))
                .andExpect(jsonPath("$.availableQuantity").value(90));

        // Then: Verify DB state
        Inventory inventory = inventoryRepository.findByVariantId(variantId).orElseThrow();
        assertThat(inventory.getReservedQuantity()).isEqualTo(10);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(90);

        // Verify reservation created
        Optional<Reservation> reservation = reservationRepository.findByOrderItemId(orderItemId);
        assertThat(reservation).isPresent();
        assertThat(reservation.get().getQuantity()).isEqualTo(10);
        assertThat(reservation.get().getStatus()).isEqualTo(Reservation.ReservationStatus.RESERVED);
    }

    @Test
    void shouldConfirmStockViaController() throws Exception {
        // Given: Reserve first
        inventoryService.reserveStock(variantId, 10, orderItemId);
        Inventory afterReserve = inventoryRepository.findByVariantId(variantId).orElseThrow();
        assertThat(afterReserve.getReservedQuantity()).isEqualTo(10);
        assertThat(afterReserve.getAvailableQuantity()).isEqualTo(90);

        String requestBody = """
            {
                "variantId": 100,
                "quantity": 10
            }
            """;

        // When
        mockMvc.perform(post("/inventory/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variantId").value(100))
                .andExpect(jsonPath("$.quantity").value(90))
                .andExpect(jsonPath("$.reservedQuantity").value(0))
                .andExpect(jsonPath("$.availableQuantity").value(90));

        // Then: Verify DB state
        Inventory inventory = inventoryRepository.findByVariantId(variantId).orElseThrow();
        assertThat(inventory.getQuantity()).isEqualTo(90);
        assertThat(inventory.getReservedQuantity()).isEqualTo(0);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(90);
    }

    @Test
    void shouldReleaseReservationViaController() throws Exception {
        // Given: Reserve first
        inventoryService.reserveStock(variantId, 10, orderItemId);
        Inventory afterReserve = inventoryRepository.findByVariantId(variantId).orElseThrow();
        assertThat(afterReserve.getReservedQuantity()).isEqualTo(10);
        assertThat(afterReserve.getAvailableQuantity()).isEqualTo(90);

        // When
        mockMvc.perform(delete("/inventory/reserve/{orderItemId}", orderItemId)
                        .param("variantId", "100")
                        .param("quantity", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // Then: Verify DB state
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Inventory inventory = inventoryRepository.findByVariantId(variantId).orElseThrow();
            assertThat(inventory.getReservedQuantity()).isEqualTo(0);
            assertThat(inventory.getAvailableQuantity()).isEqualTo(100);
        });

        // Verify reservation status updated
        Reservation reservation = reservationRepository.findByOrderItemId(orderItemId).orElseThrow();
        assertThat(reservation.getStatus()).isEqualTo(Reservation.ReservationStatus.RELEASED);
        assertThat(reservation.getReleasedAt()).isNotNull();
    }

    @Test
    void shouldGetInventoryViaController() throws Exception {
        // When
        mockMvc.perform(get("/inventory/{variantId}", variantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variantId").value(100))
                .andExpect(jsonPath("$.productId").value(50))
                .andExpect(jsonPath("$.productName").value("Test Product"))
                .andExpect(jsonPath("$.quantity").value(100))
                .andExpect(jsonPath("$.reservedQuantity").value(0))
                .andExpect(jsonPath("$.availableQuantity").value(100))
                .andExpect(jsonPath("$.reorderLevel").value(10))
                .andExpect(jsonPath("$.lowStock").value(false));
    }

    @Test
    void shouldReturn404ForNonExistentInventory() throws Exception {
        // When
        mockMvc.perform(get("/inventory/{variantId}", 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReserveThenConfirmThenReleaseFlow() throws Exception {
        // Given: Initial inventory with 100 units
        // Step 1: Reserve 20 units
        String reserveRequest = """
            {
                "variantId": 100,
                "quantity": 20,
                "orderItemId": 2000
            }
            """;

        mockMvc.perform(post("/inventory/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reserveRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservedQuantity").value(20))
                .andExpect(jsonPath("$.availableQuantity").value(80));

        // Verify reservation created
        Optional<Reservation> reservation = reservationRepository.findByOrderItemId(2000L);
        assertThat(reservation).isPresent();
        assertThat(reservation.get().getQuantity()).isEqualTo(20);

        // Step 2: Confirm 15 units (partial confirmation)
        String confirmRequest = """
            {
                "variantId": 100,
                "quantity": 15
            }
            """;

        mockMvc.perform(post("/inventory/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(85))
                .andExpect(jsonPath("$.reservedQuantity").value(5))
                .andExpect(jsonPath("$.availableQuantity").value(80));

        Inventory afterConfirm = inventoryRepository.findByVariantId(variantId).orElseThrow();
        assertThat(afterConfirm.getQuantity()).isEqualTo(85);
        assertThat(afterConfirm.getReservedQuantity()).isEqualTo(5);
        assertThat(afterConfirm.getAvailableQuantity()).isEqualTo(80);

        // Step 3: Release remaining 5 units
        mockMvc.perform(delete("/inventory/reserve/{orderItemId}", 2000L)
                        .param("variantId", "100")
                        .param("quantity", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // Verify final state
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Inventory inventory = inventoryRepository.findByVariantId(variantId).orElseThrow();
            assertThat(inventory.getQuantity()).isEqualTo(85);
            assertThat(inventory.getReservedQuantity()).isEqualTo(0);
            assertThat(inventory.getAvailableQuantity()).isEqualTo(85);
        });

        Reservation finalReservation = reservationRepository.findByOrderItemId(2000L).orElseThrow();
        assertThat(finalReservation.getStatus()).isEqualTo(Reservation.ReservationStatus.RELEASED);
        assertThat(finalReservation.getReleasedAt()).isNotNull();
    }
}
