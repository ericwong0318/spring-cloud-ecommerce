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

import java.time.LocalDateTime;
import java.util.List;

@Component
@EnableScheduling
public class ReactiveOutboxEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ReactiveOutboxEventPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final TransactionalOperator transactionalOperator;

    @Value("${outbox.publisher.batch-size:100}")
    private int batchSize;

    @Value("${outbox.publisher.max-retries:5}")
    private int maxRetries;

    @Value("${outbox.publisher.exchange:outbox.exchange}")
    private String exchange;

    public ReactiveOutboxEventPublisher(OutboxEventRepository outboxEventRepository,
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
        Flux.fromIterable(outboxEventRepository.findUnpublishedEventsWithRetryLimit(maxRetries))
                .flatMap(this::publishEventTransactional, batchSize)
                .subscribe();
    }

    private Mono<Void> publishEventTransactional(OutboxEvent event) {
        return Mono.fromRunnable(() -> {
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
                })
                .then()
                .as(transactionalOperator::transactional);
    }

    private void publishEvent(OutboxEvent event) throws JsonProcessingException {
        String routingKey = event.getAggregateType().toLowerCase() + "." + event.getEventType().toLowerCase();
        rabbitTemplate.convertAndSend(exchange, routingKey, event.getPayload());
    }

    public Mono<Void> saveEvent(String aggregateType, String aggregateId, String eventType, Object payload) {
        return Mono.fromRunnable(() -> {
                    try {
                        String jsonPayload = objectMapper.writeValueAsString(payload);
                        OutboxEvent event = new OutboxEvent(aggregateType, aggregateId, eventType, jsonPayload);
                        outboxEventRepository.save(event);
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