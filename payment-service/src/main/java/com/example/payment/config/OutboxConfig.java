package com.example.payment.config;

import com.example.common.event.OutboxEventPublisher;
import com.example.common.event.PaymentEvent;
import com.example.common.event.ReactiveIdempotentEventProcessor;
import com.example.common.event.RoutingKeyStrategy;
import com.example.payment.event.PaymentEventPublisher;
import com.example.payment.repository.ProcessedEventRepository;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Payment-service outbox configuration.
 * Provides RoutingKeyStrategy for payment-specific routing keys.
 * Uses common module's OutboxEventPublisher and ReactiveIdempotentEventProcessor.
 */
@Configuration
public class OutboxConfig {

    @Bean
    public RoutingKeyStrategy paymentRoutingKeyStrategy() {
        return (aggregateType, eventType) -> switch (eventType.toUpperCase()) {
            case "AUTHORIZED" -> "payment.authorized";
            case "CAPTURED" -> "payment.captured";
            case "REFUNDED" -> "payment.refunded";
            case "FAILED" -> "payment.failed";
            default -> "payment.unknown";
        };
    }

    @Bean
    public PaymentEventPublisher paymentEventPublisher(
            OutboxEventPublisher outboxEventPublisher,
            ReactiveIdempotentEventProcessor idempotentProcessor,
            ProcessedEventRepository processedEventRepository,
            @Value("${rabbitmq.exchange.payment}") String exchange) {
        return new PaymentEventPublisher(outboxEventPublisher, idempotentProcessor, processedEventRepository, exchange);
    }
}