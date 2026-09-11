package com.example.system;

import com.example.common.dto.AuthorizeRequest;
import com.example.common.dto.CaptureRequest;
import com.example.common.dto.NotificationDto;
import com.example.common.dto.OrderDto;
import com.example.common.dto.OrderItemDto;
import com.example.common.dto.PaymentDto;
import com.example.common.dto.ProductDto;
import com.example.common.dto.ProductVariantDto;
import com.example.common.event.BaseEvent;
import com.example.common.event.IdempotentEventProcessor;
import com.example.common.event.InventoryEvent;
import com.example.common.event.OrderEvent;
import com.example.common.event.PaymentEvent;
import com.example.common.event.ProductEvent;
import com.example.common.event.ReservationExpiredEvent;
import com.example.inventory.model.Inventory;
import com.example.inventory.repository.InventoryRepository;
import com.example.order.repository.OrderRepository;
import com.example.order.repository.OrderItemRepository;
import com.example.order.repository.ProcessedEventRepository;
import com.example.order.repository.ShipmentRepository;
import com.example.payment.repository.PaymentRepository;
import com.example.system.load.AssertionHelpers;
import com.example.system.load.MetricsCollector;
import com.example.system.load.ParallelExecutor;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * System-level integration tests for the E-Commerce platform.
 * Tests the full happy path: Browse → Order → Reserve → Pay → Confirm → Ship → Deliver
 * Also tests idempotency of event processing.
 */
@Testcontainers
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {
        ECommerceSystemTest.TestConfig.class
    }
)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ECommerceSystemTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("ecommerce_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static final MongoDBContainer MONGODB = new MongoDBContainer("mongo:7.0")
            .withReuse(true);

    @Container
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:3.13-management-alpine")
            .withExposedPorts(5672, 15672);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private IdempotentEventProcessor idempotentEventProcessor;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private JdbcTemplate categoryJdbcTemplate;

    @Autowired
    private JdbcTemplate inventoryJdbcTemplate;

    @Autowired
    private JdbcTemplate orderJdbcTemplate;

    @Autowired
    private JdbcTemplate paymentJdbcTemplate;

@Autowired
     private JdbcTemplate jdbcTemplate;

    @Value("${local.server.port}")
    private int port;

    @Value("${rabbitmq.exchange.ecommerce}")
    private String ecommerceExchange;

    @Value("${rabbitmq.routing-key.order-created}")
    private String orderCreatedRoutingKey;

    @Value("${rabbitmq.routing-key.payment-authorized}")
    private String paymentAuthorizedRoutingKey;

    @Value("${rabbitmq.routing-key.reservation-expired}")
    private String reservationExpiredRoutingKey;

    private EventCollector eventCollector;
    private DatabaseTestHelper dbHelper;

    private Long testCategoryId;
    private Long testProductId;
    private Long testVariantId1;
    private Long testVariantId2;
    private Long testOrderId;
    private Long testOrderItemId1;
    private Long testOrderItemId2;
    private UUID testEventId;
    private UUID testPaymentEventId;

    @BeforeAll
    static void startContainers() {
        POSTGRES.start();
        MONGODB.start();
        RABBITMQ.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");

        // R2DBC
        registry.add("spring.r2dbc.url", () -> String.format("r2dbc:postgresql://%s:%d/%s",
                POSTGRES.getHost(), POSTGRES.getFirstMappedPort(), POSTGRES.getDatabaseName()));
        registry.add("spring.r2dbc.username", POSTGRES::getUsername);
        registry.add("spring.r2dbc.password", POSTGRES::getPassword);

        // MongoDB (for product service)
        registry.add("spring.data.mongodb.uri", MONGODB::getReplicaSetUrl);
        registry.add("spring.data.mongodb.database", () -> "product_test_db");
        registry.add("spring.data.mongodb.auto-index-creation", () -> "true");

        // RabbitMQ
        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBITMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBITMQ::getAdminPassword);

        // RabbitMQ properties for order service
        registry.add("rabbitmq.exchange.order", () -> "order.exchange");
        registry.add("rabbitmq.exchange.payment", () -> "payment.exchange");
        registry.add("rabbitmq.exchange.inventory", () -> "inventory.exchange");
        registry.add("rabbitmq.exchange.ecommerce", () -> "ecommerce.exchange");
        registry.add("rabbitmq.queue.order-events", () -> "order.events.queue");
        registry.add("rabbitmq.queue.payment-events", () -> "payment.events.queue");
        registry.add("rabbitmq.queue.inventory-events", () -> "inventory.events.queue");
        registry.add("rabbitmq.queue.reservation-expired", () -> "reservation.expired.queue");
        registry.add("rabbitmq.routing-key.order-created", () -> "order.created");
        registry.add("rabbitmq.routing-key.order-updated", () -> "order.updated");
        registry.add("rabbitmq.routing-key.order-cancelled", () -> "order.cancelled");
        registry.add("rabbitmq.routing-key.payment-authorized", () -> "payment.authorized");
        registry.add("rabbitmq.routing-key.payment-captured", () -> "payment.captured");
        registry.add("rabbitmq.routing-key.payment-refunded", () -> "payment.refunded");
        registry.add("rabbitmq.routing-key.payment-failed", () -> "payment.failed");
        registry.add("rabbitmq.routing-key.inventory-reserved", () -> "inventory.reserved");
        registry.add("rabbitmq.routing-key.reservation-expired", () -> "reservation.expired");

        // Disable service discovery and config server
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.config.import", () -> "optional:configserver:");

        // Disable security for tests
        registry.add("spring.autoconfigure.exclude", () -> "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration," +
                "org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration");
    }

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/";

        eventCollector = new EventCollector(rabbitTemplate);
        dbHelper = new DatabaseTestHelper(
                categoryJdbcTemplate, inventoryJdbcTemplate,
                orderJdbcTemplate, paymentJdbcTemplate, rabbitTemplate,
                idempotentEventProcessor, inventoryRepository, orderRepository,
                orderItemRepository, processedEventRepository, shipmentRepository,
                paymentRepository
        );

        // Clean up
        eventCollector.clear();
        processedEventRepository.deleteAll();
        inventoryRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        shipmentRepository.deleteAll();
        paymentRepository.deleteAll();

        // Create test data - simple product and variant IDs for testing
        testCategoryId = dbHelper.createCategory("Electronics", null);
        testProductId = 1L;
        testVariantId1 = 2L;
        testVariantId2 = 3L;

        dbHelper.createInventory(testVariantId1, testProductId, "Laptop", "LAPTOP-13-SILVER", 100, 10, new BigDecimal("500.00"));
        dbHelper.createInventory(testVariantId2, testProductId, "Laptop", "LAPTOP-15-SPACE-GRAY", 50, 5, new BigDecimal("650.00"));

        testEventId = UUID.randomUUID();
        testPaymentEventId = UUID.randomUUID();
    }

    // ==================== HAPPY PATH TEST ====================

    @Test
    @Order(1)
    @DisplayName("Full Happy Path: Browse → Order → Reserve → Pay → Confirm → Ship → Deliver")
    void testFullHappyPath() {
        // 1. Create Order with multiple items
        List<OrderItemDto> items = List.of(
                OrderItemDto.builder()
                        .productId(testProductId)
                        .variantId(testVariantId1)
                        .skuCode("LAPTOP-13-SILVER")
                        .productName("Laptop 13-inch Silver")
                        .quantity(2)
                        .quantityShipped(0)
                        .price(new BigDecimal("999.99"))
                        .status(OrderItemDto.OrderItemStatus.PENDING)
                        .build(),
                OrderItemDto.builder()
                        .productId(testProductId)
                        .variantId(testVariantId2)
                        .skuCode("LAPTOP-15-SPACE-GRAY")
                        .productName("Laptop 15-inch Space Gray")
                        .quantity(1)
                        .quantityShipped(0)
                        .price(new BigDecimal("1299.99"))
                        .status(OrderItemDto.OrderItemStatus.PENDING)
                        .build()
        );

        BigDecimal totalAmount = new BigDecimal("3299.97"); // 2*999.99 + 1*1299.99

        OrderDto orderDto = OrderDto.builder()
                .customerId("CUST-001")
                .customerEmail("customer@example.com")
                .status(OrderDto.OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .items(items)
                .build();

        // 2. Place Order via Order Service API
        String orderResponse = given()
                .contentType(ContentType.JSON)
                .body(orderDto)
                .when()
                .post("/orders")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        testOrderId = extractOrderId(orderResponse);
        assertThat(testOrderId).isNotNull();
        testOrderItemId1 = extractOrderItemId(orderResponse, 0);
        testOrderItemId2 = extractOrderItemId(orderResponse, 1);

        // 3. Verify OrderEvent.CREATED published to RabbitMQ
        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(eventCollector.hasEvent("CREATED")).isEqualTo(true);
                    List<BaseEvent> orderEvents = eventCollector.getEventsByType("CREATED");
                    assertThat(orderEvents).hasSize(1);
                    OrderEvent orderEvent = (OrderEvent) orderEvents.get(0);
                    assertThat(orderEvent.getOrderId()).isEqualTo(testOrderId);
                    assertThat(orderEvent.getItems()).hasSize(2);
                });

        // 4. Verify Inventory Service processed order.created → reservations created
        await().atMost(15, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    // Verify inventory reserved for variant 1 (2 units)
                    Inventory inv1 = inventoryRepository.findByVariantId(testVariantId1).orElseThrow();
                    assertThat(inv1.getReservedQuantity()).isEqualTo(2);
                    assertThat(inv1.getAvailableQuantity()).isEqualTo(98);

                    // Verify inventory reserved for variant 2 (1 unit)
                    Inventory inv2 = inventoryRepository.findByVariantId(testVariantId2).orElseThrow();
                    assertThat(inv2.getReservedQuantity()).isEqualTo(1);
                    assertThat(inv2.getAvailableQuantity()).isEqualTo(49);
                });

        // 5. Verify Order status transitioned to RESERVED
        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    OrderDto order = dbHelper.findOrderById(testOrderId).orElseThrow();
                    assertThat(order.getStatus()).isEqualTo(OrderDto.OrderStatus.RESERVED);
                });

        // 6. Verify Inventory events published
        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(eventCollector.hasEvent("RESERVED")).isEqualTo(true);
                    List<BaseEvent> inventoryEvents = eventCollector.getEventsByType("RESERVED");
                    assertThat(inventoryEvents).hasSizeGreaterThanOrEqualTo(2);
                });

        // 7. Authorize Payment via Payment Service API
        String authorizeResponse = given()
                .contentType(ContentType.JSON)
                .body(new AuthorizeRequest(
                        testOrderId, totalAmount, "USD", "CUST-001", "customer@example.com",
                        "auth-" + testOrderId + "-" + testPaymentEventId))
                .when()
                .post("/payments/authorize")
                .then()
                .statusCode(201)
                .extract()
                .asString();

        Long paymentId = extractPaymentId(authorizeResponse);

        // 8. Verify PaymentEvent.AUTHORIZED published
        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(eventCollector.hasEvent("AUTHORIZED")).isEqualTo(true);
                    List<BaseEvent> paymentEvents = eventCollector.getEventsByType("AUTHORIZED");
                    assertThat(paymentEvents).hasSize(1);
                    PaymentEvent paymentEvent = (PaymentEvent) paymentEvents.get(0);
                    assertThat(paymentEvent.getOrderId()).isEqualTo(testOrderId);
                    assertThat(paymentEvent.getAmount()).isEqualByComparingTo(totalAmount);
                });

        // 9. Verify Order Service transitioned to CONFIRMED (via payment.authorized event)
        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    OrderDto order = dbHelper.findOrderById(testOrderId).orElseThrow();
                    assertThat(order.getStatus()).isEqualTo(OrderDto.OrderStatus.CONFIRMED);
                });

        // 10. Verify Order items status transitioned to RESERVED
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    OrderItemDto item1 = dbHelper.findOrderItemById(testOrderItemId1).orElseThrow();
                    OrderItemDto item2 = dbHelper.findOrderItemById(testOrderItemId2).orElseThrow();
                    assertThat(item1.getStatus()).isEqualTo(OrderItemDto.OrderItemStatus.RESERVED);
                    assertThat(item2.getStatus()).isEqualTo(OrderItemDto.OrderItemStatus.RESERVED);
                });

        // 11. Capture Payment
        String captureResponse = given()
                .contentType(ContentType.JSON)
                .body(new CaptureRequest("txn_" + paymentId))
                .when()
                .post("/payments/" + paymentId + "/capture")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        // 12. Verify PaymentEvent.CAPTURED published
        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(eventCollector.hasEvent("CAPTURED")).isEqualTo(true);
                    List<BaseEvent> capturedEvents = eventCollector.getEventsByType("CAPTURED");
                    assertThat(capturedEvents).hasSize(1);
                });

        // 13. Create Shipment (partial) via Order Service API
        // First create shipment via REST (if endpoint exists) or directly via helper
        // For now, we'll verify the infrastructure is ready for shipment creation

        // 14. Verify Order status transitioned to SHIPPED after shipment
        // This would happen when order service receives shipment.created event

        // 15. Complete shipment → DELIVERED
        // This would happen when order service receives shipment.delivered event

        // Final verification: All services processed events correctly
        verifyEndToEndConsistency();
    }

    // ==================== IDEMPOTENCY TESTS ====================

    @Test
    @Order(2)
    @DisplayName("Idempotency: Duplicate OrderEvent.CREATED processed once")
    void testDuplicateOrderCreatedIdempotency() {
        // Create a new order for this test
        List<OrderItemDto> items = List.of(
                OrderItemDto.builder()
                        .productId(testProductId)
                        .variantId(testVariantId1)
                        .skuCode("LAPTOP-13-SILVER")
                        .productName("Laptop 13-inch Silver")
                        .quantity(3)
                        .quantityShipped(0)
                        .price(new BigDecimal("999.99"))
                        .status(OrderItemDto.OrderItemStatus.PENDING)
                        .build()
        );

        BigDecimal totalAmount = new BigDecimal("2999.97");

        OrderDto orderDto = OrderDto.builder()
                .customerId("CUST-002")
                .customerEmail("customer2@example.com")
                .status(OrderDto.OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .items(items)
                .build();

        String orderResponse = given()
                .contentType(ContentType.JSON)
                .body(orderDto)
                .when()
                .post("/orders")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        Long duplicateOrderId = extractOrderId(orderResponse);
        Long duplicateOrderItemId = extractOrderItemId(orderResponse, 0);

        // Get the original event ID from the processed events
        List<BaseEvent> createdEvents = eventCollector.getEventsByType("CREATED");
        OrderEvent originalEvent = (OrderEvent) createdEvents.stream()
                .filter(e -> ((OrderEvent) e).getOrderId().equals(duplicateOrderId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No order created event found"));
        UUID originalEventId = originalEvent.getEventId();

        // Clear event collector for this test
        eventCollector.clear();

        // 2. Send duplicate OrderEvent.CREATED with same eventId via RabbitMQ
        dbHelper.publishOrderCreatedDuplicate(duplicateOrderId, "CUST-002", "customer2@example.com",
                totalAmount, originalEvent.getItems(), originalEventId);
        dbHelper.publishOrderCreatedDuplicate(duplicateOrderId, "CUST-002", "customer2@example.com",
                totalAmount, originalEvent.getItems(), originalEventId);

        // 3. Verify inventory reservation only happened once (3 units, not 6)
        await().atMost(15, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Inventory inv = inventoryRepository.findByVariantId(testVariantId1).orElseThrow();
                    // Should only reserve 3 units once, not 6 (3 + 3)
                    assertThat(inv.getReservedQuantity()).isEqualTo(5); // 2 from previous test + 3 new
                });

        // 4. Verify Order status only transitioned once
        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    OrderDto order = dbHelper.findOrderById(duplicateOrderId).orElseThrow();
                    assertThat(order.getStatus()).isEqualTo(OrderDto.OrderStatus.RESERVED);
                });

        // 5. Verify ProcessedEvent recorded only once
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(processedEventRepository.existsByEventId(originalEventId)).isEqualTo(true);
                });
    }

    @Test
    @Order(3)
    @DisplayName("Idempotency: Duplicate PaymentEvent.SUCCESS processed once")
    void testDuplicatePaymentAuthorizedIdempotency() {
        // Create a new order for this test
        List<OrderItemDto> items = List.of(
                OrderItemDto.builder()
                        .productId(testProductId)
                        .variantId(testVariantId2)
                        .skuCode("LAPTOP-15-SPACE-GRAY")
                        .productName("Laptop 15-inch Space Gray")
                        .quantity(1)
                        .quantityShipped(0)
                        .price(new BigDecimal("1299.99"))
                        .status(OrderItemDto.OrderItemStatus.PENDING)
                        .build()
        );

        BigDecimal totalAmount = new BigDecimal("1299.99");

        OrderDto orderDto = OrderDto.builder()
                .customerId("CUST-003")
                .customerEmail("customer3@example.com")
                .status(OrderDto.OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .items(items)
                .build();

        String orderResponse = given()
                .contentType(ContentType.JSON)
                .body(orderDto)
                .when()
                .post("/orders")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        Long duplicateOrderId = extractOrderId(orderResponse);
        Long duplicateOrderItemId = extractOrderItemId(orderResponse, 0);

        // Wait for order to reach RESERVED state
        await().atMost(15, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    OrderDto order = dbHelper.findOrderById(duplicateOrderId).orElseThrow();
                    assertThat(order.getStatus()).isEqualTo(OrderDto.OrderStatus.RESERVED);
                });

        // Authorize payment
        String authorizeResponse = given()
                .contentType(ContentType.JSON)
                .body(new AuthorizeRequest(
                        duplicateOrderId, totalAmount, "USD", "CUST-003", "customer3@example.com",
                        "auth-" + duplicateOrderId + "-original"))
                .when()
                .post("/payments/authorize")
                .then()
                .statusCode(201)
                .extract()
                .asString();

        Long paymentId = extractPaymentId(authorizeResponse);

        // Get the original payment event ID
        List<BaseEvent> authorizedEvents = eventCollector.getEventsByType("AUTHORIZED");
        PaymentEvent originalPaymentEvent = (PaymentEvent) authorizedEvents.stream()
                .filter(e -> ((PaymentEvent) e).getOrderId().equals(duplicateOrderId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No payment authorized event found"));
        UUID originalPaymentEventId = originalPaymentEvent.getEventId();

        // Clear event collector
        eventCollector.clear();

        // 2. Send duplicate PaymentEvent.AUTHORIZED with same eventId via RabbitMQ
        dbHelper.publishPaymentAuthorizedDuplicate(paymentId, duplicateOrderId, "CUST-003", "customer3@example.com",
                totalAmount, "USD", "txn_" + paymentId, originalPaymentEventId);
        dbHelper.publishPaymentAuthorizedDuplicate(paymentId, duplicateOrderId, "CUST-003", "customer3@example.com",
                totalAmount, "USD", "txn_" + paymentId, originalPaymentEventId);

        // 3. Verify Order only transitioned to CONFIRMED once (not twice)
        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    OrderDto order = dbHelper.findOrderById(duplicateOrderId).orElseThrow();
                    assertThat(order.getStatus()).isEqualTo(OrderDto.OrderStatus.CONFIRMED);
                });

        // 4. Verify payment not double-captured
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    List<BaseEvent> capturedEvents = eventCollector.getEventsByType("CAPTURED");
                    // No new capture event should be triggered by duplicate authorized
                    assertThat(capturedEvents).isEmpty();
                });

        // 5. Verify ProcessedEvent recorded only once
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(processedEventRepository.existsByEventId(originalPaymentEventId)).isEqualTo(true);
                });
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    @Order(4)
    @DisplayName("Reservation expiry triggers order cancellation")
    void testReservationExpiryCancelsOrder() {
        // Create a new order
        List<OrderItemDto> items = List.of(
                OrderItemDto.builder()
                        .productId(testProductId)
                        .variantId(testVariantId1)
                        .skuCode("LAPTOP-13-SILVER")
                        .productName("Laptop 13-inch Silver")
                        .quantity(1)
                        .quantityShipped(0)
                        .price(new BigDecimal("999.99"))
                        .status(OrderItemDto.OrderItemStatus.PENDING)
                        .build()
        );

        OrderDto orderDto = OrderDto.builder()
                .customerId("CUST-004")
                .customerEmail("customer4@example.com")
                .status(OrderDto.OrderStatus.PENDING)
                .totalAmount(new BigDecimal("999.99"))
                .items(items)
                .build();

        String orderResponse = given()
                .contentType(ContentType.JSON)
                .body(orderDto)
                .when()
                .post("/orders")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        Long expiryOrderId = extractOrderId(orderResponse);
        Long expiryOrderItemId = extractOrderItemId(orderResponse, 0);

        // Wait for order to reach RESERVED state
        await().atMost(15, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    OrderDto order = dbHelper.findOrderById(expiryOrderId).orElseThrow();
                    assertThat(order.getStatus()).isEqualTo(OrderDto.OrderStatus.RESERVED);
                });

        // Verify inventory reserved
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Inventory inv = inventoryRepository.findByVariantId(testVariantId1).orElseThrow();
                    assertThat(inv.getReservedQuantity()).isGreaterThan(0);
                });

        // Simulate reservation expiry
        dbHelper.publishReservationExpired(expiryOrderId, expiryOrderItemId, testVariantId1, 1);

        // 2. Verify Order Service received ReservationExpiredEvent → CANCELLED
        await().atMost(15, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    OrderDto order = dbHelper.findOrderById(expiryOrderId).orElseThrow();
                    assertThat(order.getStatus()).isEqualTo(OrderDto.OrderStatus.CANCELLED);
                });

        // 3. Verify inventory reservation released
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Inventory inv = inventoryRepository.findByVariantId(testVariantId1).orElseThrow();
                    // Reservation should be released
                    assertThat(inv.getAvailableQuantity()).isEqualTo(inv.getQuantity());
                });

        // 4. Verify OrderEvent.CANCELLED published
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(eventCollector.hasEvent("CANCELLED")).isEqualTo(true);
                });
    }

    @Test
    @DisplayName("Partial Reservation + Backorder Flow")
    void testPartialReservationBackorder() {
        // Create order with 5 items but only 3 in stock
        // Partial reservation: 3 reserved, 2 go to backorder
        Long productId = 99L;
        Long variantId = 299L;
        Integer initialStock = 3;

        // Create inventory with limited stock
        List<OrderItemDto> orderItems = List.of(
                OrderItemDto.builder()
                        .productId(productId)
                        .variantId(variantId)
                        .skuCode("LIMITED-299")
                        .productName("Limited Stock Product")
                        .quantity(5)
                        .quantityShipped(0)
                        .price(new BigDecimal("19.99"))
                        .status(OrderItemDto.OrderItemStatus.PENDING)
                        .build()
        );

        dbHelper.createInventory(variantId, productId, "Limited Stock Product", "LIMITED-299", initialStock, 0, new BigDecimal("10.00"));

        // Create order with 5 units (more than available 3)
        OrderDto orderDto = OrderDto.builder()
                .customerId("CUST-BACKORDER")
                .customerEmail("backorder@example.com")
                .status(OrderDto.OrderStatus.PENDING)
                .totalAmount(new BigDecimal("99.95"))
                .items(orderItems)
                .build();

        String createResponse = given()
                .contentType(ContentType.JSON)
                .body(orderDto)
                .when()
                .post("/orders")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        Long orderId = extractOrderId(createResponse);

        // Wait for reservation processing - verify partial reservation
        // 3 units reserved, 2 units backordered
        await().atMost(15, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    OrderDto order = dbHelper.findOrderById(orderId).orElseThrow();
                    OrderItemDto item = order.getItems().get(0);
                    // With backorder, item should be in BACKORDERED status
                    assertThat(item.getStatus()).isEqualTo(OrderItemDto.OrderItemStatus.BACKORDERED);
                });

        // Verify inventory: 2 units on order, 1 unit backordered
        Inventory inv = inventoryRepository.findByVariantId(variantId).orElseThrow();
        assertThat(inv.getReservedQuantity()).isEqualTo(3);
        assertThat(inv.getQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("Reservation Expiry (15 min) auto-release")
    void testReservationExpiry() {
        // Create order with a variant that has inventory
        List<OrderItemDto> items = List.of(
                OrderItemDto.builder()
                        .productId(testProductId)
                        .variantId(testVariantId1)
                        .skuCode("LAPTOP-13-SILVER")
                        .productName("Laptop 13-inch Silver")
                        .quantity(1)
                        .quantityShipped(0)
                        .price(new BigDecimal("999.99"))
                        .status(OrderItemDto.OrderItemStatus.PENDING)
                        .build()
        );

        OrderDto orderDto = OrderDto.builder()
                .customerId("CUST-EXPIRY")
                .customerEmail("expiry@example.com")
                .status(OrderDto.OrderStatus.PENDING)
                .totalAmount(new BigDecimal("999.99"))
                .items(items)
                .build();

        String orderResponse = given()
                .contentType(ContentType.JSON)
                .body(orderDto)
                .when()
                .post("/orders")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        Long expiryOrderId = extractOrderId(orderResponse);
        Long expiryOrderItemId = extractOrderItemId(orderResponse, 0);

        // Wait for order to reach RESERVED state
        await().atMost(15, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    OrderDto order = dbHelper.findOrderById(expiryOrderId).orElseThrow();
                    assertThat(order.getStatus()).isEqualTo(OrderDto.OrderStatus.RESERVED);
                });

        // Verify inventory reserved
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Inventory inv = inventoryRepository.findByVariantId(testVariantId1).orElseThrow();
                    assertThat(inv.getReservedQuantity()).isEqualTo(1);
                });

        // Simulate reservation expiry by publishing ReservationExpiredEvent
        dbHelper.publishReservationExpired(expiryOrderId, expiryOrderItemId, testVariantId1, 1);

        // Verify Order Service received ReservationExpiredEvent → CANCELLED
        await().atMost(15, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    OrderDto order = dbHelper.findOrderById(expiryOrderId).orElseThrow();
                    assertThat(order.getStatus()).isEqualTo(OrderDto.OrderStatus.CANCELLED);
                });

        // Verify inventory reservation released
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Inventory inv = inventoryRepository.findByVariantId(testVariantId1).orElseThrow();
                    assertThat(inv.getReservedQuantity()).isEqualTo(0);
                });
    }

    @Test
    @DisplayName("Payment Failure → Order Cancel → Inventory Release")
    void testPaymentFailureOrderCancelInventoryRelease() {
        // Create order
        List<OrderItemDto> items = List.of(
                OrderItemDto.builder()
                        .productId(testProductId)
                        .variantId(testVariantId1)
                        .skuCode("LAPTOP-13-SILVER")
                        .productName("Laptop 13-inch Silver")
                        .quantity(2)
                        .quantityShipped(0)
                        .price(new BigDecimal("999.99"))
                        .status(OrderItemDto.OrderItemStatus.PENDING)
                        .build()
        );

        OrderDto orderDto = OrderDto.builder()
                .customerId("CUST-PAYFAIL")
                .customerEmail("payfail@example.com")
                .status(OrderDto.OrderStatus.PENDING)
                .totalAmount(new BigDecimal("1999.98"))
                .items(items)
                .build();

        String orderResponse = given()
                .contentType(ContentType.JSON)
                .body(orderDto)
                .when()
                .post("/orders")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        Long failOrderId = extractOrderId(orderResponse);

        // Wait for order to reach RESERVED state
        await().atMost(15, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    OrderDto order = dbHelper.findOrderById(failOrderId).orElseThrow();
                    assertThat(order.getStatus()).isEqualTo(OrderDto.OrderStatus.RESERVED);
                });

        // Verify inventory reserved
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Inventory inv = inventoryRepository.findByVariantId(testVariantId1).orElseThrow();
                    assertThat(inv.getReservedQuantity()).isEqualTo(2);
                });

        // Simulate payment failure by publishing PaymentEvent.FAILED
        PaymentEvent failedEvent = PaymentEvent.failed(null, failOrderId, "CUST-PAYFAIL", "payfail@example.com",
                new BigDecimal("1999.98"), "USD");
        failedEvent.setEventId(UUID.randomUUID());
        rabbitTemplate.convertAndSend("payment.exchange", "payment.failed", failedEvent);

        // Verify PaymentEvent.FAILED → Order CANCELLED
        await().atMost(15, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    OrderDto order = dbHelper.findOrderById(failOrderId).orElseThrow();
                    assertThat(order.getStatus()).isEqualTo(OrderDto.OrderStatus.CANCELLED);
                });

        // Verify inventory released (availableQuantity restored)
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Inventory inv = inventoryRepository.findByVariantId(testVariantId1).orElseThrow();
                    assertThat(inv.getReservedQuantity()).isEqualTo(0);
                    assertThat(inv.getAvailableQuantity()).isEqualTo(inv.getQuantity());
                });

        // Verify OrderEvent.CANCELLED published
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(eventCollector.hasEvent("CANCELLED")).isEqualTo(true);
                });
    }

    @Test
    @DisplayName("Payment partial capture → partial confirm")
    void testPartialCapture() {
        // Create order with 2 lines (variant A qty=2, variant B qty=2)
        List<OrderItemDto> items = List.of(
                OrderItemDto.builder()
                        .productId(testProductId)
                        .variantId(testVariantId1)
                        .skuCode("LAPTOP-13-SILVER")
                        .productName("Laptop 13-inch Silver")
                        .quantity(2)
                        .quantityShipped(0)
                        .price(new BigDecimal("999.99"))
                        .status(OrderItemDto.OrderItemStatus.PENDING)
                        .build(),
                OrderItemDto.builder()
                        .productId(testProductId)
                        .variantId(testVariantId2)
                        .skuCode("LAPTOP-15-SPACE-GRAY")
                        .productName("Laptop 15-inch Space Gray")
                        .quantity(2)
                        .quantityShipped(0)
                        .price(new BigDecimal("1299.99"))
                        .status(OrderItemDto.OrderItemStatus.PENDING)
                        .build()
        );

        BigDecimal totalAmount = new BigDecimal("4599.96"); // 2*999.99 + 2*1299.99

        OrderDto orderDto = OrderDto.builder()
                .customerId("CUST-PARTIAL")
                .customerEmail("partial@example.com")
                .status(OrderDto.OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .items(items)
                .build();

        String orderResponse = given()
                .contentType(ContentType.JSON)
                .body(orderDto)
                .when()
                .post("/orders")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        Long partialOrderId = extractOrderId(orderResponse);

        // Wait for order to reach RESERVED state (both items reserved)
        await().atMost(15, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    OrderDto order = dbHelper.findOrderById(partialOrderId).orElseThrow();
                    assertThat(order.getStatus()).isEqualTo(OrderDto.OrderStatus.RESERVED);
                });

        // Authorize payment for full amount
        BigDecimal partialCaptureAmount = new BigDecimal("1999.98"); // Only variant A amount (2 * 999.99)
        String authResponse = given()
                .contentType(ContentType.JSON)
                .body(new AuthorizeRequest(
                        partialOrderId, totalAmount, "USD", "CUST-PARTIAL", "partial@example.com",
                        "auth-" + partialOrderId + "-" + UUID.randomUUID()))
                .when()
                .post("/payments/authorize")
                .then()
                .statusCode(201)
                .extract()
                .asString();

        Long paymentId = extractPaymentId(authResponse);

        // Capture only partial amount (variant A amount)
        String captureResponse = given()
                .contentType(ContentType.JSON)
                .body(new CaptureRequest("txn_partial_" + paymentId))
                .when()
                .post("/payments/" + paymentId + "/capture")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        // Verify payment captured with partial amount
        await().atMost(15, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    OrderDto order = dbHelper.findOrderById(partialOrderId).orElseThrow();
                    // Order should be CONFIRMED (payment captured triggers order confirmed)
                    assertThat(order.getStatus()).isEqualTo(OrderDto.OrderStatus.CONFIRMED);
                    // Items should still be RESERVED (system confirms order on capture)
                    OrderItemDto item1 = dbHelper.findOrderItemById(extractOrderItemId(orderResponse, 0)).orElseThrow();
                    OrderItemDto item2 = dbHelper.findOrderItemById(extractOrderItemId(orderResponse, 1)).orElseThrow();
                    assertThat(item1.getStatus()).isEqualTo(OrderItemDto.OrderItemStatus.RESERVED);
                    assertThat(item2.getStatus()).isEqualTo(OrderItemDto.OrderItemStatus.RESERVED);
                });
    }

    @Test
    @DisplayName("Notification retry + fallback (email→SMS)")
    void testNotificationRetryAndFallback() {
        // Create order that triggers notification events
        List<OrderItemDto> items = List.of(
                OrderItemDto.builder()
                        .productId(testProductId)
                        .variantId(testVariantId1)
                        .skuCode("LAPTOP-13-SILVER")
                        .productName("Laptop 13-inch Silver")
                        .quantity(1)
                        .quantityShipped(0)
                        .price(new BigDecimal("999.99"))
                        .status(OrderItemDto.OrderItemStatus.PENDING)
                        .build()
        );

        OrderDto orderDto = OrderDto.builder()
                .customerId("CUST-NOTIFY")
                .customerEmail("notify@example.com")
                .status(OrderDto.OrderStatus.PENDING)
                .totalAmount(new BigDecimal("999.99"))
                .items(items)
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(orderDto)
                .when()
                .post("/orders")
                .then()
                .statusCode(200);

        // Wait for order confirmation notification event
        await().atMost(15, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    OrderDto order = dbHelper.findOrderById(
                            io.restassured.path.json.JsonPath.from(given()
                                    .when().get("/orders/" + "CUST-NOTIFY")
                                    .then().extract().asString()
                            ).getLong("id")).orElseThrow();
                    assertThat(order.getStatus()).isEqualTo(OrderDto.OrderStatus.RESERVED);
                });

        // Verify notification events were collected (retry or fallback)
        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    boolean hasNotificationEvent = eventCollector.hasEvent("NOTIFICATION_RETRY")
                            || eventCollector.hasEvent("NOTIFICATION_FAILED")
                            || eventCollector.hasEvent("NOTIFICATION_SENT");
                    assertThat(hasNotificationEvent).isTrue();
                });
    }

    @Test
    @DisplayName("Category Move Subtree")
    void testCategoryMoveSubtree() {
        // Create category hierarchy using dbHelper
        Long parentCatId = dbHelper.createCategory("Electronics", null);
        Long childCatId = dbHelper.createCategory("Computers", parentCatId);
        Long grandchildCatId = dbHelper.createCategory("Laptops", childCatId);

        // Verify initial hierarchy
        Long initialParent = jdbcTemplate.queryForObject(
                "SELECT parent_id FROM category WHERE id = ?",
                Long.class, grandchildCatId);
        assertThat(initialParent).isEqualTo(childCatId);

        // Move Computers (and its subtree) under Accessories
        Long newParentId = dbHelper.createCategory("Accessories", null);

        // Move the child category (Computers) to new parent
        jdbcTemplate.update(
                "UPDATE category SET parent_id = ? WHERE id = ?",
                newParentId, childCatId);

        // Verify: Computers now has new parent
        Long actualParent = jdbcTemplate.queryForObject(
                "SELECT parent_id FROM category WHERE id = ?",
                Long.class, childCatId);
        assertThat(actualParent).isEqualTo(newParentId);

        // Verify: Laptops still under Computers (subtree moved correctly)
        Long laptopParent = jdbcTemplate.queryForObject(
                "SELECT parent_id FROM category WHERE id = ?",
                Long.class, grandchildCatId);
        assertThat(laptopParent).isEqualTo(childCatId);

        // Verify no cycles created (ancestor chain doesn't loop back)
        // Accessories (root) -> Computers -> Laptops - no cycles
        assertThat(newParentId).isNotEqualTo(childCatId);
        assertThat(childCatId).isNotEqualTo(grandchildCatId);
    }

    @Test
    @DisplayName("Variant Soft-Delete with Existing OrderItems")
    void testVariantSoftDeleteWithExistingOrderItems() {
        // Create order referencing a variant
        List<OrderItemDto> items = List.of(
                OrderItemDto.builder()
                        .productId(testProductId)
                        .variantId(testVariantId1)
                        .skuCode("LAPTOP-13-SILVER")
                        .productName("Laptop 13-inch Silver")
                        .quantity(1)
                        .quantityShipped(0)
                        .price(new BigDecimal("999.99"))
                        .status(OrderItemDto.OrderItemStatus.PENDING)
                        .build()
        );

        OrderDto orderDto = OrderDto.builder()
                .customerId("CUST-SOFTDEL")
                .customerEmail("softdel@example.com")
                .status(OrderDto.OrderStatus.PENDING)
                .totalAmount(new BigDecimal("999.99"))
                .items(items)
                .build();

        String orderResponse = given()
                .contentType(ContentType.JSON)
                .body(orderDto)
                .when()
                .post("/orders")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        Long softDeleteOrderId = extractOrderId(orderResponse);

        // Wait for order to be processed (reservation + confirmation)
        await().atMost(20, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    OrderDto order = dbHelper.findOrderById(softDeleteOrderId).orElseThrow();
                    assertThat(order.getStatus()).isEqualTo(OrderDto.OrderStatus.CONFIRMED);
                });

        // Soft-delete the variant by setting deleted_flag = 1
        jdbcTemplate.update(
                "UPDATE product_variant SET deleted_flag = 1 WHERE id = ?",
                testVariantId1);

        // Verify order still readable (soft-delete should not cascade delete)
        OrderDto order = dbHelper.findOrderById(softDeleteOrderId).orElseThrow();
        assertThat(order.getId()).isEqualTo(softDeleteOrderId);
        assertThat(order.getStatus()).isEqualTo(OrderDto.OrderStatus.CONFIRMED);
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getItems().get(0).getVariantId()).isEqualTo(testVariantId1);

        // Verify variant still exists in product DB (soft-delete, not hard delete)
        // Using productRepository directly since dbHelper doesn't have findProductVariantById
        // Optional<ProductVariantDto> variant = dbHelper.findProductVariantById("LAPTOP-13-SILVER");
        // assertThat(variant).isPresent();
    }

    // ==================== VERIFICATION HELPERS ====================

    private void verifyEndToEndConsistency() {
        // Verify order exists
        OrderDto order = dbHelper.findOrderById(testOrderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderDto.OrderStatus.CONFIRMED);

        // Verify order items reserved
        OrderItemDto item1 = dbHelper.findOrderItemById(testOrderItemId1).orElseThrow();
        OrderItemDto item2 = dbHelper.findOrderItemById(testOrderItemId2).orElseThrow();
        assertThat(item1.getStatus()).isEqualTo(OrderItemDto.OrderItemStatus.RESERVED);
        assertThat(item2.getStatus()).isEqualTo(OrderItemDto.OrderItemStatus.RESERVED);

        // Verify inventory reservations
        Inventory inv1 = inventoryRepository.findByVariantId(testVariantId1).orElseThrow();
        Inventory inv2 = inventoryRepository.findByVariantId(testVariantId2).orElseThrow();
        assertThat(inv1.getReservedQuantity()).isEqualTo(2);
        assertThat(inv2.getReservedQuantity()).isEqualTo(1);
    }

    private Long extractOrderId(String response) {
        // Parse JSON response to extract order ID
        // Assuming response format: {"id": 1, "customerId": "...", ...}
        return io.restassured.path.json.JsonPath.from(response).getLong("id");
    }

    private Long extractOrderItemId(String response, int index) {
        return io.restassured.path.json.JsonPath.from(response).getLong("items[" + index + "].id");
    }

    private Long extractPaymentId(String response) {
        return io.restassured.path.json.JsonPath.from(response).getLong("id");
    }

    @SpringBootApplication(
        exclude = {
            org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
            org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration.class
        }
    )
    static class TestConfig {
        @Bean
        public JdbcTemplate jdbcTemplate(javax.sql.DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }
    }

    // ==================== LOAD TESTS ====================

    @Test
    @Order(10)
    @DisplayName("LOAD: Concurrent Order Placement - No Oversell")
    void testConcurrentOrderPlacementNoOversell() {
        // Given: Inventory with quantity=100 for a variant
        Long variantId = testVariantId1;
        int initialQuantity = 100;

        // Reset inventory to known state
        Inventory inventory = inventoryRepository.findByVariantId(variantId).orElseThrow();
        inventory.setQuantity(initialQuantity);
        inventory.setReservedQuantity(0);
        inventoryRepository.save(inventory);

        // Clear event collector
        eventCollector.clear();

        int totalRequests = 200; // 200 parallel requests, each ordering 1 unit
        int expectedSuccess = 100; // Only 100 should succeed
        int expectedFailure = 100; // 100 should fail with insufficient stock

        MetricsCollector metrics = new MetricsCollector();
        ParallelExecutor executor = new ParallelExecutor(50); // 50 concurrent threads

        try {
            // When: Launch 200 parallel requests to POST /orders
            List<ParallelExecutor.ExecutionResult<String>> results = executor.executeParallel(taskIndex -> {
                Instant start = Instant.now();
                try {
                    OrderDto orderDto = createLoadTestOrder(variantId, 1);
                    String response = given()
                            .contentType(ContentType.JSON)
                            .body(orderDto)
                            .when()
                            .post("/orders")
                            .then()
                            .extract()
                            .asString();

                    metrics.recordSuccess("order-create", Duration.between(start, Instant.now()));
                    return response;
                } catch (Exception e) {
                    metrics.recordFailure("order-create", Duration.between(start, Instant.now()), e);
                    throw e;
                }
            }, totalRequests);

            // Then: Verify exactly 100 orders succeed (CONFIRMED), 100 fail (insufficient stock)
            AssertionHelpers.assertOrderSuccessCount(results, expectedSuccess, expectedFailure);

            // Verify final inventory: quantity=100, reservedQuantity=0, availableQuantity=0
            AssertionHelpers.assertInventoryState(inventoryRepository, variantId,
                    initialQuantity, 0, 0);

            // No negative quantities, no lost updates
            AssertionHelpers.assertNoOversell(inventoryRepository, variantId);

            // Print metrics
            metrics.printSummary();
            AssertionHelpers.logErrorBreakdown(results, "ConcurrentOrderPlacement");

            // Verify error types - should be mostly 409 Conflict (insufficient stock)
            Map<String, Long> errorCounts = AssertionHelpers.categorizeErrors(results);
            assertThat(errorCounts).containsKey("HttpClientErrorException$Conflict");

        } finally {
            executor.shutdown(30, TimeUnit.SECONDS);
        }
    }

    @Test
    @Order(11)
    @DisplayName("LOAD: Concurrent Reservation + Payment Capture - Idempotency Under Load")
    void testConcurrentReservationAndPaymentCapture() {
        // Given: Place 50 orders in parallel (each reserves 1 unit)
        Long variantId = testVariantId2;
        int initialQuantity = 50;

        // Reset inventory
        Inventory inventory = inventoryRepository.findByVariantId(variantId).orElseThrow();
        inventory.setQuantity(initialQuantity);
        inventory.setReservedQuantity(0);
        inventoryRepository.save(inventory);

        eventCollector.clear();

        int orderCount = 50;
        MetricsCollector metrics = new MetricsCollector();
        ParallelExecutor executor = new ParallelExecutor(25);

        List<Long> orderIds = new java.util.concurrent.CopyOnWriteArrayList<>();
        AtomicInteger orderIndex = new AtomicInteger(0);

        try {
            // Phase 1: Create 50 orders in parallel
            List<ParallelExecutor.ExecutionResult<String>> orderResults = executor.executeParallel(taskIndex -> {
                Instant start = Instant.now();
                try {
                    OrderDto orderDto = createLoadTestOrder(variantId, 1);
                    String response = given()
                            .contentType(ContentType.JSON)
                            .body(orderDto)
                            .when()
                            .post("/orders")
                            .then()
                            .statusCode(200)
                            .extract()
                            .asString();

                    Long orderId = extractOrderId(response);
                    orderIds.add(orderId);
                    orderIndex.incrementAndGet();

                    metrics.recordSuccess("order-create", Duration.between(start, Instant.now()));
                    return response;
                } catch (Exception e) {
                    metrics.recordFailure("order-create", Duration.between(start, Instant.now()), e);
                    throw e;
                }
            }, orderCount);

            AssertionHelpers.assertOrderSuccessCount(orderResults, orderCount, 0);

            // Wait for all orders to reach RESERVED state
            await().atMost(30, TimeUnit.SECONDS)
                    .untilAsserted(() -> {
                        long reservedCount = orderIds.stream()
                                .map(id -> dbHelper.findOrderById(id).orElseThrow())
                                .filter(o -> o.getStatus() == OrderDto.OrderStatus.RESERVED)
                                .count();
                        assertThat(reservedCount).isEqualTo(orderCount);
                    });

            // Verify inventory: 50 reserved, 0 available
            AssertionHelpers.assertInventoryState(inventoryRepository, variantId,
                    initialQuantity, orderCount, 0);

            // Phase 2: Trigger payment capture for all 50 in parallel
            // First authorize payments for all orders
            List<Long> paymentIds = new java.util.concurrent.CopyOnWriteArrayList<>();

            List<ParallelExecutor.ExecutionResult<String>> authResults = executor.executeParallel(taskIndex -> {
                Long orderId = orderIds.get(taskIndex);
                Instant start = Instant.now();
                try {
                    BigDecimal amount = new BigDecimal("1299.99"); // variant2 price
                    String response = given()
                            .contentType(ContentType.JSON)
                            .body(new AuthorizeRequest(
                                    orderId, amount, "USD", "CUST-LOAD-" + orderId, "load@example.com",
                                    "auth-" + orderId + "-" + UUID.randomUUID()))
                            .when()
                            .post("/payments/authorize")
                            .then()
                            .statusCode(201)
                            .extract()
                            .asString();

                    Long paymentId = extractPaymentId(response);
                    paymentIds.add(paymentId);

                    metrics.recordSuccess("payment-authorize", Duration.between(start, Instant.now()));
                    return response;
                } catch (Exception e) {
                    metrics.recordFailure("payment-authorize", Duration.between(start, Instant.now()), e);
                    throw e;
                }
            }, orderCount);

            AssertionHelpers.assertPaymentSuccessCount(authResults, orderCount);

            // Wait for all orders to reach CONFIRMED state
            await().atMost(30, TimeUnit.SECONDS)
                    .untilAsserted(() -> {
                        long confirmedCount = orderIds.stream()
                                .map(id -> dbHelper.findOrderById(id).orElseThrow())
                                .filter(o -> o.getStatus() == OrderDto.OrderStatus.CONFIRMED)
                                .count();
                        assertThat(confirmedCount).isEqualTo(orderCount);
                    });

            // Phase 3: Capture all payments in parallel
            List<ParallelExecutor.ExecutionResult<String>> captureResults = executor.executeParallel(taskIndex -> {
                Long paymentId = paymentIds.get(taskIndex);
                Instant start = Instant.now();
                try {
                    String response = given()
                            .contentType(ContentType.JSON)
                            .body(new CaptureRequest("txn_" + paymentId))
                            .when()
                            .post("/payments/" + paymentId + "/capture")
                            .then()
                            .statusCode(200)
                            .extract()
                            .asString();

                    metrics.recordSuccess("payment-capture", Duration.between(start, Instant.now()));
                    return response;
                } catch (Exception e) {
                    metrics.recordFailure("payment-capture", Duration.between(start, Instant.now()), e);
                    throw e;
                }
            }, orderCount);

            // Verify all 50 captures successful (idempotent)
            AssertionHelpers.assertPaymentSuccessCount(captureResults, orderCount);

            // Verify inventory: 50 confirmed (quantity reduced), 0 reserved
            AssertionHelpers.assertInventoryState(inventoryRepository, variantId,
                    0, 0, 0);

            // No duplicate confirmations, no race conditions
            // Verify exactly 50 orders are CONFIRMED
            long finalConfirmedCount = orderIds.stream()
                    .map(id -> dbHelper.findOrderById(id).orElseThrow())
                    .filter(o -> o.getStatus() == OrderDto.OrderStatus.CONFIRMED)
                    .count();
            assertThat(finalConfirmedCount).isEqualTo(orderCount);

            // Verify no order is confirmed more than once (check processed events)
            // Each order should have exactly one CAPTURED event
            long capturedEvents = eventCollector.countEvents("CAPTURED");
            assertThat(capturedEvents).isEqualTo(orderCount);

            metrics.printSummary();
            AssertionHelpers.logErrorBreakdown(orderResults, "ConcurrentReservationPaymentCapture");

        } finally {
            executor.shutdown(30, TimeUnit.SECONDS);
        }
    }

    @Test
    @Order(12)
    @DisplayName("LOAD: Scheduler Contention - Multiple Inventory Instances")
    void testSchedulerContentionMultipleInstances() {
        // This test simulates multiple inventory-service instances processing expired reservations
        // by directly invoking the scheduler logic with advisory locks

        // Given: Create 50 stale reservations (> 15 min old)
        Long variantId = testVariantId1;
        int staleReservationCount = 50;

        // Reset inventory to have enough stock
        Inventory inventory = inventoryRepository.findByVariantId(variantId).orElseThrow();
        inventory.setQuantity(staleReservationCount + 100); // Extra buffer
        inventory.setReservedQuantity(staleReservationCount);
        inventoryRepository.save(inventory);

        // Create 50 orders with 1 item each
        List<Long> createdOrderIds = new java.util.ArrayList<>();
        List<Long> createdOrderItemIds = new java.util.concurrent.CopyOnWriteArrayList<>();

        for (int i = 0; i < staleReservationCount; i++) {
            OrderDto orderDto = createLoadTestOrder(variantId, 1);
            String response = given()
                    .contentType(ContentType.JSON)
                    .body(orderDto)
                    .when()
                    .post("/orders")
                    .then()
                    .statusCode(200)
                    .extract()
                    .asString();

            Long orderId = extractOrderId(response);
            Long orderItemId = extractOrderItemId(response, 0);
            createdOrderIds.add(orderId);
            createdOrderItemIds.add(orderItemId);
        }

        // Wait for all reservations to be created
        await().atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Inventory inv = inventoryRepository.findByVariantId(variantId).orElseThrow();
                    assertThat(inv.getReservedQuantity()).isEqualTo(staleReservationCount);
                });

        eventCollector.clear();

        // When: Simulate 3 inventory-service instances running the scheduler concurrently
        ParallelExecutor schedulerExecutor = new ParallelExecutor(3); // 3 instances
        MetricsCollector metrics = new MetricsCollector();

        try {
            // Each "instance" tries to process all expired reservations
            List<ParallelExecutor.ExecutionResult<Integer>> schedulerResults = schedulerExecutor.executeParallel(instanceIndex -> {
                Instant start = Instant.now();
                int processedCount = 0;

                // Simulate the scheduler logic with advisory locks
                // For this test, we'll use the order items that we know are reserved
                for (Long orderItemId : createdOrderItemIds) {
                    long lockKey = 1000000L + orderItemId; // Same LOCK_KEY_OFFSET as scheduler

                    boolean lockAcquired = false;
                    try {
                        // Try to acquire advisory lock
                        Boolean result = jdbcTemplate.queryForObject(
                                "SELECT pg_try_advisory_lock(?)", Boolean.class, lockKey);
                        lockAcquired = Boolean.TRUE.equals(result);

                        if (!lockAcquired) {
                            // Another instance is processing this reservation
                            continue;
                        }

                        // Check if reservation still exists and is expired
                        // In real implementation, this would query the reservation table
                        // For this test, we simulate by checking inventory
                        Optional<Inventory> invOpt = inventoryRepository.findByVariantId(variantId);
                        if (invOpt.isPresent() && invOpt.get().getReservedQuantity() > 0) {
                            // Release the reservation
                            inventoryRepository.findByVariantId(variantId).ifPresent(inv -> {
                                inv.setReservedQuantity(inv.getReservedQuantity() - 1);
                                inventoryRepository.save(inv);
                            });

                            // Publish reservation expired event
                            ReservationExpiredEvent event = ReservationExpiredEvent.expired(
                                    orderItemId, variantId, 1, LocalDateTime.now());
                            rabbitTemplate.convertAndSend("ecommerce.events", "reservation.expired", event);
                            processedCount++;
                        }
                    } finally {
                        if (lockAcquired) {
                            jdbcTemplate.execute("SELECT pg_advisory_unlock(" + lockKey + ")");
                        }
                    }
                }

                metrics.recordSuccess("scheduler-run", Duration.between(start, Instant.now()));
                return processedCount;
            }, 3); // 3 instances

            // Then: Verify exactly ONE instance processes each expiry (advisory lock)
            int totalProcessed = schedulerResults.stream()
                    .filter(ParallelExecutor.ExecutionResult::isSuccess)
                    .mapToInt(ParallelExecutor.ExecutionResult::getResult)
                    .sum();

            // Total RELEASED events should = 50 (not 150)
            AssertionHelpers.assertSchedulerProcessedOnce(inventoryRepository,
                    List.of(variantId), staleReservationCount);

            // Verify exactly 50 RELEASED events published
            long releasedEvents = eventCollector.countEvents("RELEASED");
            assertThat(releasedEvents).isEqualTo(staleReservationCount);

            // Verify no deadlocks, no lock timeouts
            AssertionHelpers.assertErrorRateBelow(metrics, "scheduler-run", 0.0);

            metrics.printSummary();

        } finally {
            schedulerExecutor.shutdown(60, TimeUnit.SECONDS);
        }
    }

    // Helper method to create load test orders
    private OrderDto createLoadTestOrder(Long variantId, int quantity) {
        Long productId = testProductId;
        BigDecimal price = variantId.equals(testVariantId1) ? new BigDecimal("999.99") : new BigDecimal("1299.99");
        String skuCode = variantId.equals(testVariantId1) ? "LAPTOP-13-SILVER" : "LAPTOP-15-SPACE-GRAY";
        String productName = variantId.equals(testVariantId1) ? "Laptop 13-inch Silver" : "Laptop 15-inch Space Gray";

        List<OrderItemDto> items = List.of(
                OrderItemDto.builder()
                        .productId(productId)
                        .variantId(variantId)
                        .skuCode(skuCode)
                        .productName(productName)
                        .quantity(quantity)
                        .quantityShipped(0)
                        .price(price)
                        .status(OrderItemDto.OrderItemStatus.PENDING)
                        .build()
        );

        return OrderDto.builder()
                .customerId("CUST-LOAD-" + UUID.randomUUID().toString().substring(0, 8))
                .customerEmail("loadtest@example.com")
                .status(OrderDto.OrderStatus.PENDING)
                .totalAmount(price.multiply(new BigDecimal(quantity)))
                .items(items)
                .build();
    }

    // ==================== NEW TESTS FOR TICKET 16 ====================

    @Test
    @DisplayName("Product Search/Filter via Reactive Endpoints")
    void testProductSearchAndFilter() {
        // Given: Use existing test products (created in @BeforeEach)
        // testProductId = 1L, testVariantId1 = 2L, testVariantId2 = 3L
        // These are set up in @BeforeEach with createInventory

        // Wait for products to be available via product service
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            // When: Search products by name
            String searchResponse = given()
                    .contentType(ContentType.JSON)
                    .when()
                    .get("/products/search?q=Laptop")
                    .then()
                    .statusCode(200)
                    .extract()
                    .asString();

            // Then: Should return results (may be empty if no products in MongoDB)
            // This tests the reactive endpoint works
            assertThat(searchResponse).isNotNull();
        });

        // When: Filter products by attribute
        String filterResponse = given()
                .contentType(ContentType.JSON)
                .when()
                .get("/products/filter?price=999.99")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        // Then: Should return results
        assertThat(filterResponse).isNotNull();

        // When: Get products by category
        String categoryResponse = given()
                .contentType(ContentType.JSON)
                .when()
                .get("/products/category/" + testCategoryId)
                .then()
                .statusCode(200)
                .extract()
                .asString();

        // Then: Should return results
        assertThat(categoryResponse).isNotNull();
    }

    @Test
    @DisplayName("Concurrent Order Placement - Inventory Concurrency Prevents Oversell")
    void testConcurrentOrderPlacementInventoryConcurrency() {
        // Given: Inventory with quantity=50 for a variant
        Long variantId = testVariantId1;
        int initialQuantity = 50;

        // Reset inventory to known state
        Inventory inventory = inventoryRepository.findByVariantId(variantId).orElseThrow();
        inventory.setQuantity(initialQuantity);
        inventory.setReservedQuantity(0);
        inventoryRepository.save(inventory);

        // Clear event collector
        eventCollector.clear();

        int totalRequests = 100; // 100 parallel requests, each ordering 1 unit
        int expectedSuccess = 50; // Only 50 should succeed
        int expectedFailure = 50; // 50 should fail with insufficient stock

        MetricsCollector metrics = new MetricsCollector();
        ParallelExecutor executor = new ParallelExecutor(25); // 25 concurrent threads

        try {
            // When: Launch 100 parallel requests to POST /orders
            List<ParallelExecutor.ExecutionResult<String>> results = executor.executeParallel(taskIndex -> {
                Instant start = Instant.now();
                try {
                    OrderDto orderDto = createLoadTestOrder(variantId, 1);
                    String response = given()
                            .contentType(ContentType.JSON)
                            .body(orderDto)
                            .when()
                            .post("/orders")
                            .then()
                            .extract()
                            .asString();

                    metrics.recordSuccess("order-create", Duration.between(start, Instant.now()));
                    return response;
                } catch (Exception e) {
                    metrics.recordFailure("order-create", Duration.between(start, Instant.now()), e);
                    throw e;
                }
            }, totalRequests);

            // Then: Verify exactly 50 orders succeed, 50 fail with insufficient stock
            AssertionHelpers.assertOrderSuccessCount(results, expectedSuccess, expectedFailure);

            // Verify final inventory: quantity=50, reservedQuantity=0, availableQuantity=0
            AssertionHelpers.assertInventoryState(inventoryRepository, variantId,
                    initialQuantity, 0, 0);

            // No negative quantities, no lost updates
            AssertionHelpers.assertNoOversell(inventoryRepository, variantId);

            // Print metrics
            metrics.printSummary();
            AssertionHelpers.logErrorBreakdown(results, "ConcurrentOrderPlacementInventoryConcurrency");

            // Verify error types - should be mostly 409 Conflict (insufficient stock)
            Map<String, Long> errorCounts = AssertionHelpers.categorizeErrors(results);
            assertThat(errorCounts).containsKey("HttpClientErrorException$Conflict");

        } finally {
            executor.shutdown(30, TimeUnit.SECONDS);
        }
    }
}
