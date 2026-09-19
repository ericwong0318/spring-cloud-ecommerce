package com.example.common.listener;

import com.example.common.event.BaseEvent;
import com.example.common.event.ReactiveIdempotentEventProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

public abstract class BaseReactiveSagaListener<E extends BaseEvent> {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final ReactiveIdempotentEventProcessor idempotentEventProcessor;

    protected BaseReactiveSagaListener(ReactiveIdempotentEventProcessor idempotentEventProcessor) {
        this.idempotentEventProcessor = idempotentEventProcessor;
    }

    protected final void processEvent(E event) {
        idempotentEventProcessor.process(event, this::handleEventInternal)
                .subscribe(
                        unused -> log.debug("Successfully processed event: eventId={}, eventType={}", event.getEventId(), event.getEventType()),
                        error -> log.error("Failed to process event: eventId={}, eventType={}, error={}", event.getEventId(), event.getEventType(), error.toString())
                );
    }

    protected abstract Mono<Void> handleEventInternal(E event);
}