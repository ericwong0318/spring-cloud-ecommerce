package com.example.order.pact;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.junitsupport.State;
import com.example.order.service.OrderService;
import com.example.order.service.ShipmentService;
import com.example.order.service.PaymentProcessor;
import com.example.order.service.OrderSagaOrchestrator;
import com.example.order.repository.OrderRepository;
import com.example.order.repository.OrderItemRepository;
import com.example.order.repository.ShipmentRepository;
import com.example.order.repository.ShipmentItemRepository;
import com.example.order.mapper.OrderMapper;
import com.example.order.mapper.ShipmentMapper;
import com.example.order.service.OrderEventPublisher;
import com.example.common.event.OutboxEventPublisher;
import com.example.common.event.ReactiveIdempotentEventProcessor;
import com.example.common.event.OutboxEventRepository;
import com.example.order.outbox.R2dbcOutboxEventRepository;
import com.example.common.dto.OrderDto;
import com.example.common.dto.OrderItemDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.r2dbc.R2dbcRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import com.example.order.TestSecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {
    R2dbcAutoConfiguration.class,
    R2dbcRepositoriesAutoConfiguration.class
})
@ContextConfiguration(classes = TestSecurityConfig.class)
@ExtendWith(PactVerificationInvocationContextProvider.class)
@Provider("order-service")
@PactFolder("target/pacts,../product/target/pacts,../payment-service/target/pacts,../inventory-service/target/pacts,../notification-service/target/pacts")
class OrderPactProviderTest {

    @Configuration
    static class TestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("order_db")
            .withUsername("test")
            .withPassword("test");

    @Container
    static final RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.13-management")
            .withExposedPorts(5672);

    @MockBean
    OrderService orderService;

    @MockBean
    ShipmentService shipmentService;

    @MockBean
    PaymentProcessor paymentProcessor;

    @MockBean
    OrderSagaOrchestrator orderSagaOrchestrator;

    @MockBean
    OrderRepository orderRepository;

    @MockBean
    OrderItemRepository orderItemRepository;

    @MockBean
    ShipmentRepository shipmentRepository;

    @MockBean
    ShipmentItemRepository shipmentItemRepository;

    @MockBean
    OrderMapper orderMapper;

    @MockBean
    ShipmentMapper shipmentMapper;

    @MockBean
    OrderEventPublisher orderEventPublisher;

    @MockBean
    OutboxEventPublisher outboxEventPublisher;

    @MockBean
    ReactiveIdempotentEventProcessor reactiveIdempotentEventProcessor;

    @MockBean
    OutboxEventRepository outboxEventRepository;

    @MockBean
    R2dbcOutboxEventRepository r2dbcOutboxEventRepository;

    @MockBean
    TransactionalOperator transactionalOperator;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl());
        registry.add("spring.datasource.username", () -> postgres.getUsername());
        registry.add("spring.datasource.password", () -> postgres.getPassword());
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.r2dbc.url", () -> "r2dbc:postgresql://" + postgres.getHost() + ":" + postgres.getFirstMappedPort() + "/" + postgres.getDatabaseName());
        registry.add("spring.r2dbc.username", () -> postgres.getUsername());
        registry.add("spring.r2dbc.password", () -> postgres.getPassword());
        registry.add("spring.rabbitmq.host", () -> rabbitmq.getHost());
        registry.add("spring.rabbitmq.port", () -> rabbitmq.getAmqpPort());
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");
        registry.add("rabbitmq.exchange.payment", () -> "payment.exchange");
        registry.add("rabbitmq.exchange.order", () -> "order.exchange");
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
        registry.add("eureka.client.enabled", () -> "false");
    }

    @LocalServerPort
    private int port;

    @BeforeEach
    void before(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
        Mockito.reset(orderService);
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("valid order create request")
    void validOrderCreateRequest(Map<String, Object> params) {
    }

    @State("valid order update request")
    void validOrderUpdateRequest(Map<String, Object> params) {
    }

    @State("order create request - customerId blank")
    void orderCreateRequestCustomerIdBlank(Map<String, Object> params) {
    }

    @State("order create request - customerEmail invalid")
    void orderCreateRequestCustomerEmailInvalid(Map<String, Object> params) {
    }

    @State("order create request - totalAmount null")
    void orderCreateRequestTotalAmountNull(Map<String, Object> params) {
    }

    @State("order create request - items empty")
    void orderCreateRequestItemsEmpty(Map<String, Object> params) {
    }

    @State("valid order creation request")
    void validOrderCreationRequest(Map<String, Object> params) {
        OrderItemDto item = new OrderItemDto(
            1L, 1L, 1L, "SKU-001", "Laptop Pro 15",
            2, 0, new BigDecimal("29.99"),
            OrderItemDto.OrderItemStatus.PENDING,
            LocalDateTime.now()
        );
        OrderDto orderDto = new OrderDto(
            1L,
            "cust-123",
            "customer@example.com",
            OrderDto.OrderStatus.CREATED,
            new BigDecimal("59.98"),
            List.of(item),
            List.of(),
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        Mockito.when(orderService.createOrder(Mockito.any())).thenReturn(Mono.just(orderDto));
    }

    @State("order with id 1 exists")
    void orderWithId1Exists(Map<String, Object> params) {
        OrderItemDto item = new OrderItemDto(
            1L, 1L, 1L, "SKU-001", "Laptop Pro 15",
            2, 0, new BigDecimal("29.99"),
            OrderItemDto.OrderItemStatus.PENDING,
            LocalDateTime.now()
        );
        OrderDto orderDto = new OrderDto(
            1L,
            "cust-123",
            "customer@example.com",
            OrderDto.OrderStatus.CREATED,
            new BigDecimal("59.98"),
            List.of(item),
            List.of(),
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        Mockito.when(orderService.getOrderById(1L)).thenReturn(Mono.just(orderDto));
    }

    @State("order with id 1 exists and can be cancelled")
    void orderWithId1ExistsAndCanBeCancelled(Map<String, Object> params) {
        OrderItemDto item = new OrderItemDto(
            1L, 1L, 1L, "SKU-001", "Laptop Pro 15",
            2, 0, new BigDecimal("29.99"),
            OrderItemDto.OrderItemStatus.PENDING,
            LocalDateTime.now()
        );
        OrderDto orderDto = new OrderDto(
            1L,
            "cust-123",
            "customer@example.com",
            OrderDto.OrderStatus.CANCELLED,
            new BigDecimal("59.98"),
            List.of(item),
            List.of(),
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        Mockito.when(orderService.cancelOrder(1L)).thenReturn(Mono.just(orderDto));
    }
}