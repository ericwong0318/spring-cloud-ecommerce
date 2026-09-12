package com.example.order;

import com.example.common.dto.OrderDto;
import com.example.common.dto.OrderItemDto;
import com.example.common.event.InventoryEvent;
import com.example.common.event.OrderEvent;
import com.example.common.event.PaymentEvent;
import com.example.common.event.ReservationExpiredEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class OrderIntegrationTest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/orders";
        RestAssured.port = port;
        RestAssured.basePath = "/orders";
        objectMapper.registerModule(new JavaTimeModule());
    }

    private OrderDto createOrderDto(String customerId, String customerEmail, BigDecimal totalAmount, List<OrderItemDto> items) {
        return new OrderDto(
                null,
                customerId,
                customerEmail,
                OrderDto.OrderStatus.PENDING,
                totalAmount,
                items,
                null,
                null,
                null
        );
    }

    private OrderItemDto createOrderItemDto(Long productId, Long variantId, Integer quantity, BigDecimal price) {
        return new OrderItemDto(
                null,
                productId,
                variantId,
                null,
                null,
                quantity,
                0,
                price,
                OrderItemDto.OrderItemStatus.PENDING,
                null
        );
    }

    @Test
    void createOrder_shouldCreateOrderAndPublishEvent() {
        OrderDto orderDto = createOrderDto("CUST-001", "customer@example.com",
                new BigDecimal("1999.98"),
                List.of(createOrderItemDto(1L, 1L, 2, new BigDecimal("999.99"))));

        OrderDto createdOrder = given()
                .contentType(ContentType.JSON)
                .body(orderDto)
                .when()
                .post()
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(OrderDto.class);

        assertThat(createdOrder.id()).isNotNull();
        assertThat(createdOrder.customerId()).isEqualTo("CUST-001");
        assertThat(createdOrder.status()).isEqualTo(OrderDto.OrderStatus.PENDING);
        assertThat(createdOrder.items()).hasSize(1);
        assertThat(createdOrder.items().get(0).variantId()).isEqualTo(1L);
        assertThat(createdOrder.items().get(0).status()).isEqualTo(OrderItemDto.OrderItemStatus.PENDING);
    }

    @Test
    void getOrder_shouldReturnOrder_whenExists() {
        // First create an order
        OrderDto orderDto = createOrderDto("CUST-002", "customer2@example.com",
                new BigDecimal("999.99"),
                List.of(createOrderItemDto(2L, 2L, 1, new BigDecimal("999.99"))));

        OrderDto createdOrder = given()
                .contentType(ContentType.JSON)
                .body(orderDto)
                .when()
                .post()
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(OrderDto.class);

        // Then get the order
        OrderDto retrievedOrder = given()
                .when()
                .get("/{id}", createdOrder.id())
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(OrderDto.class);

        assertThat(retrievedOrder.id()).isEqualTo(createdOrder.id());
        assertThat(retrievedOrder.customerId()).isEqualTo("CUST-002");
    }

    @Test
    void cancelOrder_shouldCancelOrder_whenPending() {
        // First create an order
        OrderDto orderDto = createOrderDto("CUST-003", "customer3@example.com",
                new BigDecimal("49.99"),
                List.of(createOrderItemDto(3L, 3L, 1, new BigDecimal("49.99"))));

        OrderDto createdOrder = given()
                .contentType(ContentType.JSON)
                .body(orderDto)
                .when()
                .post()
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(OrderDto.class);

        // Cancel the order
        OrderDto cancelledOrder = given()
                .when()
                .post("/{id}/cancel", createdOrder.id())
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(OrderDto.class);

        assertThat(cancelledOrder.status()).isEqualTo(OrderDto.OrderStatus.CANCELLED);
        assertThat(cancelledOrder.items().get(0).status()).isEqualTo(OrderItemDto.OrderItemStatus.CANCELLED);
    }

    @Test
    void cancelOrder_shouldReturn409_whenOrderNotPending() {
        // First create an order
        OrderDto orderDto = createOrderDto("CUST-004", "customer4@example.com",
                new BigDecimal("49.99"),
                List.of(createOrderItemDto(4L, 4L, 1, new BigDecimal("49.99"))));

        OrderDto createdOrder = given()
                .contentType(ContentType.JSON)
                .body(orderDto)
                .when()
                .post()
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(OrderDto.class);

        // First cancel - should succeed
        given()
                .when()
                .post("/{id}/cancel", createdOrder.id())
                .then()
                .statusCode(HttpStatus.OK.value());

        // Second cancel - should fail with 409 Conflict
        given()
                .when()
                .post("/{id}/cancel", createdOrder.id())
                .then()
                .statusCode(HttpStatus.CONFLICT.value());
    }

    @Test
    void fullSaga_shouldTransitionOrderToConfirmed_whenPaymentCaptured() {
        // Create order
        OrderDto orderDto = createOrderDto("CUST-005", "customer5@example.com",
                new BigDecimal("1999.98"),
                List.of(createOrderItemDto(5L, 5L, 2, new BigDecimal("999.99"))));

        OrderDto createdOrder = given()
                .contentType(ContentType.JSON)
                .body(orderDto)
                .when()
                .post()
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(OrderDto.class);

        Long orderId = createdOrder.id();

        // Simulate inventory reservation success
        InventoryEvent reservedEvent = InventoryEvent.reserved(5L, 5L, 2, 0);
        reservedEvent.setEventId(UUID.randomUUID());
        rabbitTemplate.convertAndSend("inventory.exchange", "reserved", reservedEvent);

        // Wait for order to transition to RESERVED
        await().untilAsserted(() -> {
            OrderDto order = given()
                    .when()
                    .get("/{id}", orderId)
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .extract()
                    .as(OrderDto.class);
            assertThat(order.status()).isEqualTo(OrderDto.OrderStatus.RESERVED);
            assertThat(order.items().get(0).status()).isEqualTo(OrderItemDto.OrderItemStatus.RESERVED);
        });

        // Simulate payment captured
        PaymentEvent paymentEvent = new PaymentEvent();
        paymentEvent.setEventType("CAPTURED");
        paymentEvent.setEventId(UUID.randomUUID());
        paymentEvent.setOrderId(orderId);
        paymentEvent.setPaymentId(100L);
        paymentEvent.setCustomerId("CUST-005");
        paymentEvent.setCustomerEmail("customer5@example.com");
        paymentEvent.setAmount(new BigDecimal("1999.98"));
        paymentEvent.setCurrency("USD");
        paymentEvent.setStatus(PaymentEvent.PaymentStatus.CAPTURED);
        paymentEvent.setTransactionId("txn_123456");
        paymentEvent.setTimestamp(LocalDateTime.now());

        rabbitTemplate.convertAndSend("payment.exchange", "payment.captured", paymentEvent);

        // Wait for order to transition to CONFIRMED
        await().untilAsserted(() -> {
            OrderDto order = given()
                    .when()
                    .get("/{id}", orderId)
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .extract()
                    .as(OrderDto.class);
            assertThat(order.status()).isEqualTo(OrderDto.OrderStatus.CONFIRMED);
            assertThat(order.items().get(0).status()).isEqualTo(OrderItemDto.OrderItemStatus.RESERVED);
        });
    }

    @Test
    void fullSaga_shouldCancelOrder_whenPaymentFailed() {
        // Create order
        OrderDto orderDto = createOrderDto("CUST-006", "customer6@example.com",
                new BigDecimal("49.99"),
                List.of(createOrderItemDto(6L, 6L, 1, new BigDecimal("49.99"))));

        OrderDto createdOrder = given()
                .contentType(ContentType.JSON)
                .body(orderDto)
                .when()
                .post()
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(OrderDto.class);

        Long orderId = createdOrder.id();

        // Simulate inventory reservation success
        InventoryEvent reservedEvent = InventoryEvent.reserved(6L, 6L, 1, 0);
        reservedEvent.setEventId(UUID.randomUUID());
        rabbitTemplate.convertAndSend("inventory.exchange", "reserved", reservedEvent);

        // Wait for order to transition to RESERVED
        await().untilAsserted(() -> {
            OrderDto order = given()
                    .when()
                    .get("/{id}", orderId)
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .extract()
                    .as(OrderDto.class);
            assertThat(order.status()).isEqualTo(OrderDto.OrderStatus.RESERVED);
        });

        // Simulate payment failed
        PaymentEvent paymentEvent = new PaymentEvent();
        paymentEvent.setEventType("FAILED");
        paymentEvent.setEventId(UUID.randomUUID());
        paymentEvent.setOrderId(orderId);
        paymentEvent.setPaymentId(101L);
        paymentEvent.setCustomerId("CUST-006");
        paymentEvent.setCustomerEmail("customer6@example.com");
        paymentEvent.setAmount(new BigDecimal("49.99"));
        paymentEvent.setCurrency("USD");
        paymentEvent.setStatus(PaymentEvent.PaymentStatus.FAILED);
        paymentEvent.setTransactionId("txn_failed");
        paymentEvent.setTimestamp(LocalDateTime.now());

        rabbitTemplate.convertAndSend("payment.exchange", "payment.failed", paymentEvent);

        // Wait for order to transition to CANCELLED
        await().untilAsserted(() -> {
            OrderDto order = given()
                    .when()
                    .get("/{id}", orderId)
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .extract()
                    .as(OrderDto.class);
            assertThat(order.status()).isEqualTo(OrderDto.OrderStatus.CANCELLED);
            assertThat(order.items().get(0).status()).isEqualTo(OrderItemDto.OrderItemStatus.CANCELLED);
        });
    }

    @Test
    void fullSaga_shouldCancelOrder_whenReservationExpired() {
        // Create order
        OrderDto orderDto = createOrderDto("CUST-007", "customer7@example.com",
                new BigDecimal("999.99"),
                List.of(createOrderItemDto(7L, 7L, 1, new BigDecimal("999.99"))));

        OrderDto createdOrder = given()
                .contentType(ContentType.JSON)
                .body(orderDto)
                .when()
                .post()
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(OrderDto.class);

        Long orderId = createdOrder.id();
        Long orderItemId = createdOrder.items().get(0).id();

        // Simulate inventory reservation success
        InventoryEvent reservedEvent = InventoryEvent.reserved(7L, 7L, 1, 0);
        reservedEvent.setEventId(UUID.randomUUID());
        rabbitTemplate.convertAndSend("inventory.exchange", "reserved", reservedEvent);

        // Wait for order to transition to RESERVED
        await().untilAsserted(() -> {
            OrderDto order = given()
                    .when()
                    .get("/{id}", orderId)
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .extract()
                    .as(OrderDto.class);
            assertThat(order.status()).isEqualTo(OrderDto.OrderStatus.RESERVED);
            assertThat(order.items().get(0).status()).isEqualTo(OrderItemDto.OrderItemStatus.RESERVED);
        });

        // Simulate reservation expired
        ReservationExpiredEvent expiredEvent = ReservationExpiredEvent.expired(orderItemId, 7L, 1, LocalDateTime.now());
        expiredEvent.setEventId(UUID.randomUUID());
        rabbitTemplate.convertAndSend("inventory.exchange", "reservation.expired", expiredEvent);

        // Wait for order to transition to CANCELLED
        await().untilAsserted(() -> {
            OrderDto order = given()
                    .when()
                    .get("/{id}", orderId)
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .extract()
                    .as(OrderDto.class);
            assertThat(order.status()).isEqualTo(OrderDto.OrderStatus.CANCELLED);
            assertThat(order.items().get(0).status()).isEqualTo(OrderItemDto.OrderItemStatus.CANCELLED);
        });
    }

    @Test
    void partialReservation_shouldSetItemToBackordered() {
        // Create order with two items
        OrderDto orderDto = createOrderDto("CUST-008", "customer8@example.com",
                new BigDecimal("1049.98"),
                List.of(
                        createOrderItemDto(8L, 8L, 1, new BigDecimal("999.99")),
                        createOrderItemDto(9L, 9L, 1, new BigDecimal("49.99"))
                ));

        OrderDto createdOrder = given()
                .contentType(ContentType.JSON)
                .body(orderDto)
                .when()
                .post()
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(OrderDto.class);

        Long orderId = createdOrder.id();

        // Simulate partial reservation - first item fully reserved, second item backordered
        InventoryEvent reservedEvent1 = InventoryEvent.reserved(8L, 8L, 1, 0);
        reservedEvent1.setEventId(UUID.randomUUID());
        rabbitTemplate.convertAndSend("inventory.exchange", "reserved", reservedEvent1);

        InventoryEvent reservedEvent2 = InventoryEvent.reserved(9L, 9L, 0, 1);
        reservedEvent2.setEventId(UUID.randomUUID());
        rabbitTemplate.convertAndSend("inventory.exchange", "reserved", reservedEvent2);

        // Wait for order to transition
        await().untilAsserted(() -> {
            OrderDto order = given()
                    .when()
                    .get("/{id}", orderId)
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .extract()
                    .as(OrderDto.class);
                    // Order should be RESERVED (one item RESERVED, one BACKORDERED)
                    assertThat(order.status()).isEqualTo(OrderDto.OrderStatus.RESERVED);
                    assertThat(order.items()).hasSize(2);
                    // Find the backordered item
                    boolean hasBackordered = order.items().stream()
                            .anyMatch(item -> item.status() == OrderItemDto.OrderItemStatus.BACKORDERED);
                    assertThat(hasBackordered).isTrue();
        });
    }
}