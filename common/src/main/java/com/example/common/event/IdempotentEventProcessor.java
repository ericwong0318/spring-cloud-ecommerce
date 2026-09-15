package com.example.common.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Consumer;

@Component
public class IdempotentEventProcessor {

    private static final Logger log = LoggerFactory.getLogger(IdempotentEventProcessor.class);

    private final ProcessedEventRepository processedEventRepository;

    public IdempotentEventProcessor(ProcessedEventRepository processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }

    public <T extends BaseEvent> void process(T event, Consumer<T> handler) {
        UUID eventId = event.getEventId();
        if (eventId == null) {
            log.warn("Event missing eventId, processing without idempotency check: {}", event.getEventType());
            handler.accept(event);
            return;
        }

        if (processedEventRepository.existsByEventId(eventId)) {
            log.info("Duplicate event detected, skipping: eventId={}, eventType={}", eventId, event.getEventType());
            return;
        }

        try {
            handler.accept(event);
            processedEventRepository.save(new ProcessedEvent(eventId));
            log.debug("Processed event and saved idempotency key: eventId={}", eventId);
        } catch (Exception e) {
            log.error("Failed to process event: eventId={}, eventType={}", eventId, event.getEventType(), e);
            throw e;
        }
    }
}
