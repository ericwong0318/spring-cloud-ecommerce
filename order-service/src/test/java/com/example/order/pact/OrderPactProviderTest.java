package com.example.order.pact;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.junitsupport.State;
import com.example.order.controller.OrderController;
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
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.r2dbc.R2dbcRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.reactive.ReactiveOAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.security.reactive.ReactiveManagementWebSecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import com.example.order.TestSecurityConfig;
import org.springframework.test.context.DynamicPropertySource;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {com.example.order.OrderServiceApplication.class, OrderController.class, OrderPactProviderTest.TestConfig.class})
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {
    org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.r2dbc.R2dbcRepositoriesAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class,
    org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.oauth2.resource.reactive.ReactiveOAuth2ResourceServerAutoConfiguration.class,
    org.springframework.boot.actuate.autoconfigure.security.reactive.ReactiveManagementWebSecurityAutoConfiguration.class
})
@Import({TestSecurityConfig.class, OrderPactProviderTest.TestConfig.class})
@ExtendWith(PactVerificationInvocationContextProvider.class)
@Provider("order-service")
@PactFolder("/Users/ericw/spring-cloud-ecommerce-worktrees/main/order-service/target/pacts")
class OrderPactProviderTest {

    @Configuration
    @EnableWebFlux
    static class TestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
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

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
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
