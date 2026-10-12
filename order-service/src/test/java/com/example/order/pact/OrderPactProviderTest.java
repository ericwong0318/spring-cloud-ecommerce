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
import com.example.order.service.OrderSagaOrchestratorImpl;
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
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.data.r2dbc.R2dbcRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.reactive.ReactiveOAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.security.reactive.ReactiveManagementWebSecurityAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import com.example.order.TestSecurityConfig;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    classes = {com.example.order.OrderServiceApplication.class, OrderPactProviderTest.TestConfig.class},
    properties = {
        "spring.main.web-application-type=reactive",
        "eureka.client.enabled=false",
        "rabbitmq.exchange.order=order.exchange",
        "rabbitmq.exchange.payment=payment.exchange",
        "rabbitmq.exchange.inventory=inventory.exchange",
        "rabbitmq.exchange.ecommerce=ecommerce.exchange",
        "rabbitmq.queue.order-events=order.events.queue",
        "rabbitmq.queue.payment-events=payment.events.queue",
        "rabbitmq.queue.inventory-events=inventory.events.queue",
        "rabbitmq.queue.reservation-expired=reservation.expired.queue",
        "rabbitmq.routing-key.order-created=order.created",
        "rabbitmq.routing-key.order-updated=order.updated",
        "rabbitmq.routing-key.order-cancelled=order.cancelled",
        "rabbitmq.routing-key.payment-authorized=payment.authorized",
        "rabbitmq.routing-key.payment-captured=payment.captured",
        "rabbitmq.routing-key.payment-refunded=payment.refunded",
        "rabbitmq.routing-key.payment-failed=payment.failed",
        "rabbitmq.routing-key.inventory-reserved=inventory.reserved",
        "rabbitmq.routing-key.reservation-expired=reservation.expired"
    }
)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {
    R2dbcAutoConfiguration.class,
    R2dbcRepositoriesAutoConfiguration.class,
    JpaRepositoriesAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class,
    RabbitAutoConfiguration.class,
    DataSourceAutoConfiguration.class,
    SecurityAutoConfiguration.class,
    OAuth2ResourceServerAutoConfiguration.class,
    UserDetailsServiceAutoConfiguration.class,
    ManagementWebSecurityAutoConfiguration.class,
    ReactiveSecurityAutoConfiguration.class,
    ReactiveOAuth2ResourceServerAutoConfiguration.class,
    ReactiveManagementWebSecurityAutoConfiguration.class
})
@Import({TestSecurityConfig.class, OrderPactProviderTest.TestConfig.class})
@ExtendWith(PactVerificationInvocationContextProvider.class)
@Provider("order-service")
@PactFolder("target/pacts")
class OrderPactProviderTest {

    @Configuration
    static class TestConfig {
        @Bean
        ConnectionFactory connectionFactory() {
            return Mockito.mock(ConnectionFactory.class);
        }
    }

    @MockBean
    OrderService orderService;

    @MockBean
    ShipmentService shipmentService;

    @MockBean
    PaymentProcessor paymentProcessor;

    @MockBean
    OrderSagaOrchestratorImpl orderSagaOrchestrator;

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
        // The createOrder method takes a CreateOrderRequest, not just an ID
        // We need to match any CreateOrderRequest
        Mockito.when(orderService.createOrder(Mockito.any())).thenReturn(Mono.just(orderDto));
    }
}
