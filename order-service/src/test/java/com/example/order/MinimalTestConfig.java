package com.example.order;

import com.example.order.mapper.OrderMapper;
import com.example.order.mapper.ShipmentMapper;
import com.example.order.service.OrderSagaOrchestratorImpl;
import com.example.order.service.ShipmentService;
import com.example.order.service.PaymentProcessor;
import com.example.order.service.OrderEventPublisher;
import com.example.common.event.OutboxEventPublisher;
import com.example.order.outbox.R2dbcOutboxEventRepository;
import com.example.order.repository.OrderItemRepository;
import com.example.order.repository.OrderRepository;
import com.example.order.repository.ShipmentRepository;
import com.example.order.service.OrderService;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.example.common.dto.OrderDto;
import com.example.common.dto.OrderItemDto;
import com.example.common.dto.CreateOrderRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Mono;

/**
 * Minimal test configuration that only includes order-service beans, avoiding common module's JPA classes.
 */
@Configuration
@ComponentScan(
    basePackages = {
        "com.example.order.controller",
        "com.example.order.service",
        "com.example.order.repository",
        "com.example.order.mapper",
        "com.example.order.model",
        "com.example.order.config",
        "com.example.order.listener"
    },
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
            com.example.common.event.OutboxEventRepository.class,
            com.example.common.event.ReactiveOutboxEventPublisher.class,
            com.example.common.event.IdempotentEventProcessor.class,
            com.example.order.service.OrderService.class,
            com.example.order.service.ShipmentService.class,
            com.example.order.outbox.R2dbcOutboxEventRepository.class,
        }),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.example.order.pact.*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.example.order.*PactProviderTest.*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.example.order.*PactConsumerTest.*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.example.order.*PactTestConfig.*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.example.order.*TestConfig.*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.example.order.BaseIntegrationTest.*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.example.order.controller.ShipmentController"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.example.order.service.ShipmentService"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.example.order.mapper.Shipment.*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.example.order.repository.Shipment.*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.example.order.model.Shipment.*")
    }
)
public class MinimalTestConfig {
    @Bean
    @Primary
    public OutboxEventPublisher outboxEventPublisher() {
        return emptyMonoMock(OutboxEventPublisher.class);
    }

    @Bean
    @Primary
    public OrderSagaOrchestratorImpl orderSagaOrchestrator(OutboxEventPublisher outboxEventPublisher,
                                                            PaymentProcessor paymentProcessor,
                                                            OrderEventPublisher orderEventPublisher,
                                                            OrderRepository orderRepository,
                                                            OrderItemRepository orderItemRepository,
                                                            OrderMapper orderMapper,
                                                            ObjectMapper objectMapper,
                                                            TransactionalOperator transactionalOperator) {
        // Use real OrderSagaOrchestratorImpl with mocked external dependencies
        return new OrderSagaOrchestratorImpl(orderRepository, orderItemRepository, orderMapper, objectMapper,
                transactionalOperator, outboxEventPublisher, paymentProcessor, orderEventPublisher);
    }

    @Bean
    @Primary
    public OrderEventPublisher orderEventPublisher() {
        return emptyMonoMock(OrderEventPublisher.class);
    }

    @Bean
    @Primary
    public ShipmentMapper shipmentMapper() {
        return Mockito.mock(ShipmentMapper.class);
    }

    @Bean
    @Primary
    public ShipmentService shipmentService() {
        return Mockito.mock(ShipmentService.class);
    }

    /**
     * Creates a real OrderService with real database repositories but mocked external services.
     * This allows integration tests to use the real database while mocking saga orchestration,
     * payment processing, and event publishing.
     */
    @Bean
    @Primary
    public OrderService orderService(OrderRepository orderRepository,
                                      OrderItemRepository orderItemRepository,
                                      OrderMapper orderMapper,
                                      ObjectMapper objectMapper,
                                      TransactionalOperator transactionalOperator,
                                      OrderSagaOrchestratorImpl sagaOrchestrator,
                                      PaymentProcessor paymentProcessor,
                                      OrderEventPublisher eventPublisher) {
        return new OrderService(orderRepository, orderItemRepository, orderMapper, objectMapper,
                transactionalOperator, sagaOrchestrator, paymentProcessor, eventPublisher, true);
    }

    /**
     * Creates a Mockito mock whose {@code Mono}-returning methods default to
     * {@link Mono#empty()}. Mockito's default {@code null} answer breaks reactive chains,
     * because a {@code flatMap}/mapper function must never return {@code null}.
     */
    private static <T> T emptyMonoMock(Class<T> type) {
        return Mockito.mock(type, invocation -> {
            if (Mono.class.isAssignableFrom(invocation.getMethod().getReturnType())) {
                return Mono.empty();
            }
            return Mockito.RETURNS_DEFAULTS.answer(invocation);
        });
    }
}