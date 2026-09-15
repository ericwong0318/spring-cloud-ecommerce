package com.example.order.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@EnableScheduling
public class R2dbcOutboxEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(R2dbcOutboxEventPublisher.class);

    private final R2dbcOutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final TransactionalOperator transactionalOperator;
    private final R2dbcOutboxEventPublisherProperties properties;

    @Autowired
    public R2dbcOutboxEventPublisher(R2dbcOutboxEventRepository outboxEventRepository,
                                     RabbitTemplate rabbitTemplate,
                                     ObjectMapper objectMapper,
                                     R2dbcTransactionManager transactionManager,
                                     R2dbcOutboxEventPublisherProperties properties) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.transactionalOperator = TransactionalOperator.create(transactionManager);
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${outbox.publisher.poll-interval-ms:5000}")
    public void publishUnpublishedEvents() {
        log.debug("Polling for unpublished outbox events");
        Flux<R2dbcOutboxEvent> events = outboxEventRepository.findUnpublishedEventsWithRetryLimit(properties.getMaxRetries())
                .take(properties.getBatchSize());

        events.flatMap(this::publishEvent)
                .onErrorContinue((e, event) -> log.error("Failed to publish event: {}", event, e))
                .subscribe();
    }

    private Mono<Void> publishEvent(R2dbcOutboxEvent event) {
        return transactionalOperator.transactional(Mono.defer(() -> {
            try {
                String routingKey = determineRoutingKey(event.getEventType());
                rabbitTemplate.convertAndSend(properties.getExchange(), routingKey, event.getPayload());
                log.info("Published event {} to RabbitMQ with routing key: {}", event.getId(), routingKey);

                event.markPublished();
                return outboxEventRepository.save(event).then();
            } catch (Exception e) {
                log.error("Failed to publish event {}: {}", event.getId(), e.getMessage());
                event.incrementRetryCount();
                return outboxEventRepository.save(event)
                        .then(Mono.error(e));
            }
        })).onErrorResume(e -> {
            log.error("Transaction failed for event {}: {}", event.getId(), e.getMessage());
            return Mono.empty();
        });
    }

    public Mono<Void> saveEvent(String aggregateType, String aggregateId, String eventType, Object payload) {
        return transactionalOperator.transactional(Mono.defer(() -> {
            R2dbcOutboxEvent event = new R2dbcOutboxEvent(aggregateType, aggregateId, eventType, serializePayload(payload));
            return outboxEventRepository.save(event).then();
        }));
    }

    private String serializePayload(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize payload: {}", payload, e);
            throw new RuntimeException("Failed to serialize payload", e);
        }
    }

    private String determineRoutingKey(String eventType) {
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
            default -> eventType.toLowerCase().replace('_', '.');
        };
    }
}