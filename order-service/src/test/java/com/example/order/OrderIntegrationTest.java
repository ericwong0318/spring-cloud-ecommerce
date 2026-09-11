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
        baseUrl = "http://localhost:" + port + "/api/v1/orders";
        RestAssured.port = port;
        RestAssured.basePath = "/api/v1/orders";
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void createOrder_shouldCreateOrderAndPublishEvent() {
        OrderDto orderDto = OrderDto.builder()
                .customerId("CUST-001")
                .customerEmail("customer@example.com")
                .status(OrderDto.OrderStatus.PENDING)
                .totalAmount(new BigDecimal("1999.98"))
                .items(List.of(
                        OrderItemDto.builder()
                                .productId(1L)
                                .variantId(1L)
                                .quantity(2)
                                .price(new BigDecimal("999.99"))
                                .build()))
                .build();

        OrderDto createdOrder = given()
                .contentType(ContentType.JSON)
                .body(orderDto)
                .when()
                .post()
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(OrderDto.class);

        assertThat(createdOrder.getId()).isNotNull();
        assertThat(createdOrder.getCustomerId()).isEqualTo("CUST-001");
        assertThat(createdOrder.getStatus()).isEqualTo(OrderDto.OrderStatus.PENDING);
        assertThat(createdOrder.getItems()).hasSize(1);
        assertThat(createdOrder.getItems().get(0).getVariantId()).isEqualTo(1L);
        assertThat(createdOrder.getItems().get(0).getStatus()).isEqualTo(OrderItemDto.OrderItemStatus.PENDING);
    }

    @Test
    void getOrder_shouldReturnOrder_whenExists() {
        // First create an order
        OrderDto orderDto = OrderDto.builder()
                .customerId("CUST-002")
                .customerEmail("customer2@example.com")
                .status(OrderDto.OrderStatus.PENDING)
                .totalAmount(new BigDecimal("999.99"))
                .items(List.of(
                        OrderItemDto.builder()
                                .productId(2L)
                                .variantId(2L)
                                .quantity(1)
                                .price(new BigDecimal("999.99"))
                                .build()))
                .build();

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
                .get("/{id}", createdOrder.getId())
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(OrderDto.class);

        assertThat(retrievedOrder.getId()).isEqualTo(createdOrder.getId());
        assertThat(retrievedOrder.getCustomerId()).isEqualTo("CUST-002");
    }

    @Test
    void cancelOrder_shouldCancelOrder_whenPending() {
        // First create an order
        OrderDto orderDto = OrderDto.builder()
                .customerId("CUST-003")
                .customerEmail("customer3@example.com")
                .status(OrderDto.OrderStatus.PENDING)
                .totalAmount(new BigDecimal("49.99"))
                .items(List.of(
                        OrderItemDto.builder()
                                .productId(3L)
                                .variantId(3L)
                                .quantity(1)
                                .price(new BigDecimal("49.99"))
                                .build()))
                .build();

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
                .post("/{id}/cancel", createdOrder.getId())
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(OrderDto.class);

        assertThat(cancelledOrder.getStatus()).isEqualTo(OrderDto.OrderStatus.CANCELLED);
        assertThat(cancelledOrder.getItems().get(0).getStatus()).isEqualTo(OrderItemDto.OrderItemStatus.CANCELLED);
    }

    @Test
    void cancelOrder_shouldReturn409_whenOrderNotPending() {
        // First create an order
        OrderDto orderDto = OrderDto.builder()
                .customerId("CUST-004")
                .customerEmail("customer4@example.com")
                .status(OrderDto.OrderStatus.PENDING)
                .totalAmount(new BigDecimal("49.99"))
                .items(List.of(
                        OrderItemDto.builder()
                                .productId(4L)
                                .variantId(4L)
                                .quantity(1)
                                .price(new BigDecimal("49.99"))
                                .build()))
                .build();

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
                .post("/{id}/cancel", createdOrder.getId())
                .then()
                .statusCode(HttpStatus.OK.value());

        // Second cancel - should fail with 409 Conflict
        given()
                .when()
                .post("/{id}/cancel", createdOrder.getId())
                .then()
                .statusCode(HttpStatus.CONFLICT.value());
    }

    @Test
    void fullSaga_shouldTransitionOrderToConfirmed_whenPaymentCaptured() {
        // Create order
        OrderDto orderDto = OrderDto.builder()
                .customerId("CUST-005")
                .customerEmail("customer5@example.com")
                .status(OrderDto.OrderStatus.PENDING)
                .totalAmount(new BigDecimal("1999.98"))
                .items(List.of(
                        OrderItemDto.builder()
                                .productId(5L)
                                .variantId(5L)
                                .quantity(2)
                                .price(new BigDecimal("999.99"))
                                .build()))
                .build();

        OrderDto createdOrder = given()
                .contentType(ContentType.JSON)
                .body(orderDto)
                .when()
                .post()
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(OrderDto.class);

        Long orderId = createdOrder.getId();

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
            assertThat(order.getStatus()).isEqualTo(OrderDto.OrderStatus.RESERVED);
            assertThat(order.getItems().get(0).getStatus()).isEqualTo(OrderItemDto.OrderItemStatus.RESERVED);
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
            assertThat(order.getStatus()).isEqualTo(OrderDto.OrderStatus.CONFIRMED);
            assertThat(order.getItems().get(0).getStatus()).isEqualTo(OrderItemDto.OrderItemStatus.RESERVED);
        });
    }

    @Test
    void fullSaga_shouldCancelOrder_whenPaymentFailed() {
        // Create order
        OrderDto orderDto = OrderDto.builder()
                .customerId("CUST-006")
                .customerEmail("customer6@example.com")
                .status(OrderDto.OrderStatus.PENDING)
                .totalAmount(new BigDecimal("49.99"))
                .items(List.of(
                        OrderItemDto.builder()
                                .productId(6L)
                                .variantId(6L)
                                .quantity(1)
                                .price(new BigDecimal("49.99"))
                                .build()))
                .build();

        OrderDto createdOrder = given()
                .contentType(ContentType.JSON)
                .body(orderDto)
                .when()
                .post()
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(OrderDto.class);

        Long orderId = createdOrder.getId();

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
            assertThat(order.getStatus()).isEqualTo(OrderDto.OrderStatus.RESERVED);
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
            assertThat(order.getStatus()).isEqualTo(OrderDto.OrderStatus.CANCELLED);
            assertThat(order.getItems().get(0).getStatus()).isEqualTo(OrderItemDto.OrderItemStatus.CANCELLED);
        });
    }

    @Test
    void fullSaga_shouldCancelOrder_whenReservationExpired() {
        // Create order
        OrderDto orderDto = OrderDto.builder()
                .customerId("CUST-007")
                .customerEmail("customer7@example.com")
                .status(OrderDto.OrderStatus.PENDING)
                .totalAmount(new BigDecimal("999.99"))
                .items(List.of(
                        OrderItemDto.builder()
                                .productId(7L)
                                .variantId(7L)
                                .quantity(1)
                                .price(new BigDecimal("999.99"))
                                .build()))
                .build();

        OrderDto createdOrder = given()
                .contentType(ContentType.JSON)
                .body(orderDto)
                .when()
                .post()
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(OrderDto.class);

        Long orderId = createdOrder.getId();
        Long orderItemId = createdOrder.getItems().get(0).getId();

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
            assertThat(order.getStatus()).isEqualTo(OrderDto.OrderStatus.RESERVED);
            assertThat(order.getItems().get(0).getStatus()).isEqualTo(OrderItemDto.OrderItemStatus.RESERVED);
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
            assertThat(order.getStatus()).isEqualTo(OrderDto.OrderStatus.CANCELLED);
            assertThat(order.getItems().get(0).getStatus()).isEqualTo(OrderItemDto.OrderItemStatus.CANCELLED);
        });
    }

    @Test
    void partialReservation_shouldSetItemToBackordered() {
        // Create order with two items
        OrderDto orderDto = OrderDto.builder()
                .customerId("CUST-008")
                .customerEmail("customer8@example.com")
                .status(OrderDto.OrderStatus.PENDING)
                .totalAmount(new BigDecimal("1049.98"))
                .items(List.of(
                        OrderItemDto.builder()
                                .productId(8L)
                                .variantId(8L)
                                .quantity(1)
                                .price(new BigDecimal("999.99"))
                                .build(),
                        OrderItemDto.builder()
                                .productId(9L)
                                .variantId(9L)
                                .quantity(1)
                                .price(new BigDecimal("49.99"))
                                .build()))
                .build();

        OrderDto createdOrder = given()
                .contentType(ContentType.JSON)
                .body(orderDto)
                .when()
                .post()
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(OrderDto.class);

        Long orderId = createdOrder.getId();

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
                    assertThat(order.getStatus()).isEqualTo(OrderDto.OrderStatus.RESERVED);
                    assertThat(order.getItems()).hasSize(2);
                    // Find the backordered item
                    boolean hasBackordered = order.getItems().stream()
                            .anyMatch(item -> item.getStatus() == OrderItemDto.OrderItemStatus.BACKORDERED);
                    assertThat(hasBackordered).isTrue();
        });
    }
}
