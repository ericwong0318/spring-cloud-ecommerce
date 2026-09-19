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
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@EnableScheduling
public class JpaOutboxEventPublisher implements OutboxEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(JpaOutboxEventPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final OutboxPublisherProperties properties;
    private final RoutingKeyStrategy routingKeyStrategy;

    public JpaOutboxEventPublisher(OutboxEventRepository outboxEventRepository,
                                    RabbitTemplate rabbitTemplate,
                                    ObjectMapper objectMapper,
                                    TransactionTemplate transactionTemplate,
                                    OutboxPublisherProperties properties,
                                    RoutingKeyStrategy routingKeyStrategy) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
        this.properties = properties;
        this.routingKeyStrategy = routingKeyStrategy;
    }

    @Override
    @Scheduled(fixedDelayString = "${outbox.publisher.poll-interval-ms:5000}")
    @SchedulerLock(name = "outboxPublisher", lockAtLeastFor = "30s", lockAtMostFor = "5m")
    public void publishOutboxEvents() {
        List<OutboxEvent> events = outboxEventRepository.findUnpublishedEventsWithRetryLimit(properties.getMaxRetries());
        if (events.isEmpty()) {
            return;
        }

        if (events.size() > properties.getBatchSize()) {
            events = events.subList(0, Math.min(properties.getBatchSize(), events.size()));
        }

        log.debug("Publishing {} outbox events", events.size());

        for (OutboxEvent event : events) {
            transactionTemplate.execute(status -> {
                try {
                    publishEvent(event);
                    event.markPublished();
                    outboxEventRepository.save(event);
                    log.debug("Published outbox event: id={}, type={}", event.getId(), event.getEventType());
                } catch (Exception e) {
                    log.error("Failed to publish outbox event: id={}, type={}", event.getId(), event.getEventType(), e);
                    event.incrementRetryCount();
                    outboxEventRepository.save(event);
                }
                return null;
            });
        }
    }

    @Override
    public Mono<Void> publishOutboxEventsReactive() {
        return Mono.fromRunnable(this::publishOutboxEvents).then();
    }

    @Override
    public Mono<Void> saveEvent(String aggregateType, String aggregateId, String eventType, Object payload) {
        return Mono.fromRunnable(() -> transactionTemplate.execute(status -> {
            try {
                String jsonPayload = objectMapper.writeValueAsString(payload);
                OutboxEvent event = new OutboxEvent(aggregateType, aggregateId, eventType, jsonPayload);
                outboxEventRepository.save(event);
                log.debug("Saved outbox event: aggregateType={}, aggregateId={}, eventType={}", aggregateType, aggregateId, eventType);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize outbox event payload", e);
                throw new RuntimeException("Failed to serialize outbox event payload", e);
            }
            return null;
        })).then();
    }

    private void publishEvent(OutboxEvent event) throws JsonProcessingException {
        String routingKey = determineRoutingKey(event.getAggregateType(), event.getEventType());
        rabbitTemplate.convertAndSend(properties.getExchange(), routingKey, event.getPayload());
    }

    protected String determineRoutingKey(String aggregateType, String eventType) {
        return routingKeyStrategy.determineRoutingKey(aggregateType, eventType);
    }
}
