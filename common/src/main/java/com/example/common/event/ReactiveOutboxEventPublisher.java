package com.example.common.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${outbox.publisher.batch-size:100}")
    private int batchSize;

    @Value("${outbox.publisher.max-retries:5}")
    private int maxRetries;

    @Value("${outbox.publisher.exchange:outbox.exchange}")
    private String exchange;

    public ReactiveOutboxEventPublisher(ReactiveOutboxEventRepository<T> outboxEventRepository,
                                         RabbitTemplate rabbitTemplate,
                                         ObjectMapper objectMapper,
                                         TransactionalOperator transactionalOperator) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.transactionalOperator = transactionalOperator;
    }

    @Scheduled(fixedDelayString = "${outbox.publisher.poll-interval-ms:5000}")
    @SchedulerLock(name = "outboxPublisher", lockAtLeastFor = "30s", lockAtMostFor = "5m")
    public void publishOutboxEvents() {
        publishOutboxEventsReactive().subscribe();
    }

    @Override
    public Mono<Void> publishOutboxEventsReactive() {
        return outboxEventRepository.findUnpublishedEventsWithRetryLimit(maxRetries)
                .take(batchSize)
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
        rabbitTemplate.convertAndSend(exchange, routingKey, event.getPayload());
    }

    /**
     * Determine the RabbitMQ routing key for an event.
     * Can be overridden by service-specific implementations.
     * Default implementation uses aggregateType.eventType format.
     */
    protected String determineRoutingKey(String aggregateType, String eventType) {
        return aggregateType.toLowerCase() + "." + eventType.toLowerCase();
    }

    public Mono<Void> saveEvent(String aggregateType, String aggregateId, String eventType, Object payload) {
        return Mono.fromRunnable(() -> {
                    try {
                        String jsonPayload = objectMapper.writeValueAsString(payload);
                        OutboxEvent event = new OutboxEvent(aggregateType, aggregateId, eventType, jsonPayload);
                        // Note: This creates a base OutboxEvent, subclasses should override saveEvent to create their specific type
                        outboxEventRepository.save((T) event).subscribe();
                        log.debug("Saved outbox event: aggregateType={}, aggregateId={}, eventType={}", aggregateType, aggregateId, eventType);
                    } catch (JsonProcessingException e) {
                        log.error("Failed to serialize outbox event payload", e);
                        throw new RuntimeException("Failed to serialize outbox event payload", e);
                    }
                })
                .then()
                .as(transactionalOperator::transactional);
    }
}