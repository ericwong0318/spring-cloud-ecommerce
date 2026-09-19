package com.example.common.event;

import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

public interface OutboxEventPublisher {

    Mono<Void> saveEvent(String aggregateType, String aggregateId, String eventType, Object payload);

    void publishOutboxEvents();

    Mono<Void> publishOutboxEventsReactive();

    // Default routing key logic
    static String determineRoutingKey(String aggregateType, String eventType) {
        return aggregateType.toLowerCase() + "." + eventType.toLowerCase();
    }
}