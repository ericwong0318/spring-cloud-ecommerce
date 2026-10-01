package com.example.order.pact;

import com.example.common.event.OutboxEventPublisher;
import com.example.common.event.OutboxEventRepository;
import com.example.common.event.ReactiveIdempotentEventProcessor;
import com.example.order.mapper.OrderMapper;
import com.example.order.mapper.ShipmentMapper;
import com.example.order.model.Order;
import com.example.order.model.OrderItem;
import com.example.order.model.Shipment;
import com.example.order.model.ShipmentItem;
import com.example.order.repository.OrderItemRepository;
import com.example.order.repository.OrderRepository;
import com.example.order.repository.ShipmentItemRepository;
import com.example.order.repository.ShipmentRepository;
import com.example.order.service.OrderSagaOrchestrator;
import com.example.order.service.OrderSagaOrchestratorImpl;
import com.example.order.service.OrderService;
import com.example.order.service.PaymentProcessor;
import com.example.order.service.PaymentProcessorImpl;
import com.example.order.service.ShipmentService;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.data.r2dbc.R2dbcRepositoriesAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableAutoConfiguration(exclude = {
    SecurityAutoConfiguration.class,
    ManagementWebSecurityAutoConfiguration.class,
    OAuth2ResourceServerAutoConfiguration.class,
    UserDetailsServiceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class,
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class,
    R2dbcRepositoriesAutoConfiguration.class
})
public class OrderPactTestConfig {

    @Bean
    @Primary
    public OutboxEventRepository outboxEventRepository() {
        return Mockito.mock(OutboxEventRepository.class);
    }

    @Bean
    @Primary
    public OutboxEventPublisher outboxEventPublisher(OutboxEventRepository outboxEventRepository) {
        return new OutboxEventPublisher() {
            @Override
            public Mono<Void> saveEvent(String aggregateType, String aggregateId, String eventType, Object payload) {
                return Mono.empty();
            }

            @Override
            public void publishOutboxEvents() {
            }

            @Override
            public Mono<Void> publishOutboxEventsReactive() {
                return Mono.empty();
            }
        };
    }

    @Bean
    @Primary
    public R2dbcTransactionManager r2dbcTransactionManager(ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
    }

    @Bean
    @Primary
    public TransactionalOperator transactionalOperator(R2dbcTransactionManager transactionManager) {
        return TransactionalOperator.create(transactionManager);
    }

    @Bean
    @Primary
    public PaymentProcessor paymentProcessor() {
        return Mockito.mock(PaymentProcessorImpl.class);
    }

    @Bean
    @Primary
    public OrderSagaOrchestrator orderSagaOrchestrator() {
        return Mockito.mock(OrderSagaOrchestratorImpl.class);
    }

    @Bean
    @Primary
    public OrderService orderService() {
        return Mockito.mock(OrderService.class);
    }

    @Bean
    @Primary
    public OrderRepository orderRepository() {
        return Mockito.mock(OrderRepository.class);
    }

    @Bean
    @Primary
    public OrderItemRepository orderItemRepository() {
        return Mockito.mock(OrderItemRepository.class);
    }

    @Bean
    @Primary
    public OrderMapper orderMapper() {
        return Mockito.mock(OrderMapper.class);
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    @Primary
    public com.example.order.service.OrderEventPublisher orderEventPublisher() {
        return Mockito.mock(com.example.order.service.OrderEventPublisher.class);
    }

    @Bean
    @Primary
    public ShipmentService shipmentService() {
        return Mockito.mock(ShipmentService.class);
    }

    @Bean
    @Primary
    public ShipmentRepository shipmentRepository() {
        return Mockito.mock(ShipmentRepository.class);
    }

    @Bean
    @Primary
    public ShipmentItemRepository shipmentItemRepository() {
        return Mockito.mock(ShipmentItemRepository.class);
    }

    @Bean
    @Primary
    public ShipmentMapper shipmentMapper() {
        return Mockito.mock(ShipmentMapper.class);
    }

    @Bean
    @Primary
    public ReactiveIdempotentEventProcessor reactiveIdempotentEventProcessor() {
        return Mockito.mock(ReactiveIdempotentEventProcessor.class);
    }
}