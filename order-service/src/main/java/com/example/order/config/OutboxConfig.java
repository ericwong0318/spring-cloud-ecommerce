package com.example.order.config;

import com.example.common.event.OutboxEventPublisher;
import com.example.common.event.OutboxPublisherProperties;
import com.example.common.event.ReactiveOutboxEventPublisher;
import com.example.common.event.ReactiveOutboxEventRepository;
import com.example.common.event.RoutingKeyStrategy;
import com.example.order.outbox.R2dbcOutboxEvent;
import com.example.order.outbox.R2dbcOutboxEventRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Order-service outbox configuration.
 * Provides a RoutingKeyStrategy bean for order-service specific routing key logic.
 * The OutboxEventPublisher is auto-configured by common module.
 */
@Configuration
public class OutboxConfig {

    @Bean
    public RoutingKeyStrategy orderRoutingKeyStrategy() {
        return (aggregateType, eventType) -> {
            // Preserve original order-service routing key logic
            return switch (eventType.toUpperCase()) {
                case "ORDER_CREATED" -> "order.created";
                case "ORDER_UPDATED" -> "order.updated";
                case "ORDER_CANCELLED" -> "order.cancelled";
                case "ORDER_CONFIRMED" -> "order.confirmed";
                case "ORDER_SHIPPED" -> "order.shipped";
                case "ORDER_DELIVERED" -> "order.delivered";
                case "PAYMENT_AUTHORIZED" -> "payment.authorized";
                case "PAYMENT_CAPTURED" -> "payment.captured";
                case "PAYMENT_REFUNDED" -> "payment.refunded";
                case "PAYMENT_FAILED" -> "payment.failed";
                case "INVENTORY_RESERVED" -> "inventory.reserved";
                case "INVENTORY_RELEASED" -> "inventory.released";
                case "INVENTORY_CONFIRMED" -> "inventory.confirmed";
                case "RESERVATION_EXPIRED" -> "reservation.expired";
                default -> aggregateType.toLowerCase() + "." + eventType.toLowerCase().replace('_', '.');
            };
        };
    }

    /**
     * Adapter to bridge R2dbcOutboxEventRepository to ReactiveOutboxEventRepository<R2dbcOutboxEvent>
     * Required for the common module's auto-configuration to work with order-service's repository.
     */
    @Bean
    @Primary
    public ReactiveOutboxEventRepository<R2dbcOutboxEvent> reactiveOutboxEventRepository(
            R2dbcOutboxEventRepository outboxEventRepository) {

        return new ReactiveOutboxEventRepository<>() {
            @Override
            public Flux<R2dbcOutboxEvent> findUnpublishedEvents() {
                return outboxEventRepository.findUnpublishedEvents();
            }

            @Override
            public Flux<R2dbcOutboxEvent> findUnpublishedEventsWithRetryLimit(int maxRetries) {
                return outboxEventRepository.findUnpublishedEventsWithRetryLimit(maxRetries);
            }

            @Override
            public Mono<Boolean> existsById(UUID id) {
                return outboxEventRepository.existsById(id);
            }

            @Override
            public Mono<R2dbcOutboxEvent> save(R2dbcOutboxEvent event) {
                return outboxEventRepository.save(event);
            }
        };
    }

    /**
     * Order-service {@link OutboxEventPublisher} that persists {@link R2dbcOutboxEvent} instances
     * (rather than the common module's base {@link OutboxEvent}), so that outbox events written by
     * {@code ReactiveOutboxEventPublisher.saveEvent} match the {@code event_outbox} table columns
     * expected by {@link R2dbcOutboxEventRepository}.
     *
     * <p>Marked {@link Primary} so order-service components inject this publisher instead of the one
     * auto-configured by {@code OutboxPublisherAutoConfiguration} (which cannot know about
     * order-service's {@code R2dbcOutboxEvent} subtype).
     */
    @Bean
    @Primary
    public OutboxEventPublisher orderOutboxEventPublisher(
            ReactiveOutboxEventRepository<R2dbcOutboxEvent> outboxEventRepository,
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper,
            R2dbcTransactionManager transactionManager,
            OutboxPublisherProperties properties,
            RoutingKeyStrategy routingKeyStrategy) {

        TransactionalOperator transactionalOperator = TransactionalOperator.create(transactionManager);
        return new ReactiveOutboxEventPublisher<R2dbcOutboxEvent>(
                outboxEventRepository,
                rabbitTemplate,
                objectMapper,
                transactionalOperator,
                properties,
                routingKeyStrategy) {

            @Override
            protected R2dbcOutboxEvent createOutboxEvent(
                    String aggregateType, String aggregateId, String eventType, String payload) {
                return new R2dbcOutboxEvent(aggregateType, aggregateId, eventType, payload);
            }
        };
    }
}