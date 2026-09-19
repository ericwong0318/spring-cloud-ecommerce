package com.example.payment.event;

import com.example.common.event.OutboxEventPublisher;
import com.example.common.event.PaymentEvent;
import com.example.common.event.ReactiveIdempotentEventProcessor;
import com.example.payment.domain.ProcessedEvent;
import com.example.payment.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Payment event publisher using common module infrastructure.
 * Delegates to OutboxEventPublisher for reliable event publishing
 * and ReactiveIdempotentEventProcessor for idempotency.
 */
@Component
public class PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventPublisher.class);

    private final OutboxEventPublisher outboxEventPublisher;
    private final ReactiveIdempotentEventProcessor idempotentProcessor;
    private final ProcessedEventRepository processedEventRepository;
    private final String exchange;

    public PaymentEventPublisher(OutboxEventPublisher outboxEventPublisher,
                                  ReactiveIdempotentEventProcessor idempotentProcessor,
                                  ProcessedEventRepository processedEventRepository,
                                  @org.springframework.beans.factory.annotation.Value("${rabbitmq.exchange.payment}") String exchange) {
        this.outboxEventPublisher = outboxEventPublisher;
        this.idempotentProcessor = idempotentProcessor;
        this.processedEventRepository = processedEventRepository;
        this.exchange = exchange;
    }

    public Mono<Void> publish(PaymentEvent event) {
        String routingKey = switch (event.getEventType()) {
            case "AUTHORIZED" -> "payment.authorized";
            case "CAPTURED" -> "payment.captured";
            case "REFUNDED" -> "payment.refunded";
            case "FAILED" -> "payment.failed";
            default -> "payment.unknown";
        };

        UUID eventId = event.getEventId();

        // Use common idempotent processor for duplicate detection
        return idempotentProcessor.process(event, e -> {
            CorrelationData correlationData = new CorrelationData(eventId.toString());

            return Mono.fromRunnable(() -> {
                outboxEventPublisher.saveEvent("Payment", eventId.toString(), event.getEventType(), e).block();
                log.info("Saved PaymentEvent {} to outbox with routing key {}", eventId, routingKey);
            })
            .then(processedEventRepository.save(eventId, java.time.LocalDateTime.now()))
            .then();
        });
    }
}