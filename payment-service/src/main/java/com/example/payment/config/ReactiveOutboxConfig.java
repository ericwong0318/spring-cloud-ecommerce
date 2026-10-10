package com.example.payment.config;

import com.example.common.event.OutboxEvent;
import com.example.common.event.OutboxPublisherProperties;
import com.example.common.event.ReactiveIdempotentEventProcessor;
import com.example.common.event.ReactiveOutboxEventPublisher;
import com.example.common.event.ReactiveOutboxEventRepository;
import com.example.payment.repository.PaymentReactiveOutboxEventRepository;
import com.example.payment.repository.ProcessedEventRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Configuration
@EnableConfigurationProperties(OutboxPublisherProperties.class)
public class ReactiveOutboxConfig {

    @Bean
    public ReactiveOutboxEventRepository<OutboxEvent> reactiveOutboxEventRepository(R2dbcEntityTemplate template) {
        return new PaymentReactiveOutboxEventRepository(template);
    }

    @Bean
    public ReactiveOutboxEventPublisher<OutboxEvent> reactiveOutboxEventPublisher(
            ReactiveOutboxEventRepository<OutboxEvent> reactiveOutboxEventRepository,
            org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper,
            org.springframework.transaction.reactive.TransactionalOperator transactionalOperator,
            OutboxPublisherProperties properties,
            com.example.common.event.RoutingKeyStrategy routingKeyStrategy) {
        return new ReactiveOutboxEventPublisher<>(
                reactiveOutboxEventRepository,
                rabbitTemplate,
                objectMapper,
                transactionalOperator,
                properties,
                routingKeyStrategy);
    }

    @Bean
    public ReactiveIdempotentEventProcessor reactiveIdempotentEventProcessor(
            ProcessedEventRepository processedEventRepository,
            R2dbcTransactionManager transactionManager) {
        TransactionalOperator transactionalOperator = TransactionalOperator.create(transactionManager);
        return new ReactiveIdempotentEventProcessor(processedEventRepository, transactionalOperator);
    }
}