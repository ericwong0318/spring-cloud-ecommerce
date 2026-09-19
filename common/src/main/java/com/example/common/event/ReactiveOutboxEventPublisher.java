package com.example.common.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@EnableScheduling
public class ReactiveOutboxEventPublisher<T extends OutboxEvent> implements OutboxEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ReactiveOutboxEventPublisher.class);

    private final ReactiveOutboxEventRepository<T> outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final TransactionalOperator transactionalOperator;
    private final OutboxPublisherProperties properties;
    private final RoutingKeyStrategy routingKeyStrategy;

    public ReactiveOutboxEventPublisher(ReactiveOutboxEventRepository<T> outboxEventRepository,
                                         RabbitTemplate rabbitTemplate,
                                         ObjectMapper objectMapper,
                                         TransactionalOperator transactionalOperator,
                                         OutboxPublisherProperties properties,
                                         RoutingKeyStrategy routingKeyStrategy) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.transactionalOperator = transactionalOperator;
        this.properties = properties;
        this.routingKeyStrategy = routingKeyStrategy;
    }

    @Scheduled(fixedDelayString = "${outbox.publisher.poll-interval-ms:5000}")
    @SchedulerLock(name = "outboxPublisher", lockAtLeastFor = "30s", lockAtMostFor = "5m")
    public void publishOutboxEvents() {
        publishOutboxEventsReactive().subscribe();
    }

    @Override
    public Mono<Void> publishOutboxEventsReactive() {
        return outboxEventRepository.findUnpublishedEventsWithRetryLimit(properties.getMaxRetries())
                .take(properties.getBatchSize())
                .flatMap(this::publishEventTransactional)
                .then();
    }

    private Mono<Void> publishEventTransactional(T event) {
        return Mono.fromRunnable(() -> {
                    try {
                        publishEvent(event);
                        event.markPublished();
                        outboxEventRepository.save(event).subscribe();
                        log.debug("Published outbox event: id={}, type={}", event.getId(), event.getEventType());
                    } catch (Exception e) {
                        log.error("Failed to publish outbox event: id={}, type={}", event.getId(), event.getEventType(), e);
                        event.incrementRetryCount();
                        outboxEventRepository.save(event).subscribe();
                    }
                })
                .then()
                .as(transactionalOperator::transactional);
    }

    private void publishEvent(T event) throws JsonProcessingException {
        String routingKey = determineRoutingKey(event.getAggregateType(), event.getEventType());
        rabbitTemplate.convertAndSend(properties.getExchange(), routingKey, event.getPayload());
    }

    /**
     * Determine the RabbitMQ routing key for an event.
     * Can be overridden by service-specific implementations.
     * Default implementation uses aggregateType.eventType format.
     */
    protected String determineRoutingKey(String aggregateType, String eventType) {
        return routingKeyStrategy.determineRoutingKey(aggregateType, eventType);
    }

    public Mono<Void> saveEvent(String aggregateType, String aggregateId, String eventType, Object payload) {
        return Mono.fromRunnable(() -> {
                    try {
                        String jsonPayload = objectMapper.writeValueAsString(payload);
                        T event = createOutboxEvent(aggregateType, aggregateId, eventType, jsonPayload);
                        outboxEventRepository.save(event).subscribe();
                        log.debug("Saved outbox event: aggregateType={}, aggregateId={}, eventType={}", aggregateType, aggregateId, eventType);
                    } catch (JsonProcessingException e) {
                        log.error("Failed to serialize outbox event payload", e);
                        throw new RuntimeException("Failed to serialize outbox event payload", e);
                    }
                })
                .then()
                .as(transactionalOperator::transactional);
    }

    /**
     * Creates an outbox event instance. Override this method to create service-specific event types.
     * Default implementation creates a base {@link OutboxEvent}.
     */
    protected T createOutboxEvent(String aggregateType, String aggregateId, String eventType, String payload) {
        return (T) new OutboxEvent(aggregateType, aggregateId, eventType, payload);
    }
}