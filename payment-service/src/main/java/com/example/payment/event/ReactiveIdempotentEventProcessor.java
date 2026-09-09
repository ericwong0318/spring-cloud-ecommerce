package com.example.payment.event;

import com.example.common.event.BaseEvent;
import com.example.payment.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.function.Function;

@Component
public class ReactiveIdempotentEventProcessor {

    private static final Logger log = LoggerFactory.getLogger(ReactiveIdempotentEventProcessor.class);

    private final ProcessedEventRepository processedEventRepository;
    private final TransactionalOperator transactionalOperator;

    public ReactiveIdempotentEventProcessor(ProcessedEventRepository processedEventRepository,
                                             TransactionalOperator transactionalOperator) {
        this.processedEventRepository = processedEventRepository;
        this.transactionalOperator = transactionalOperator;
    }

    public <T extends BaseEvent> Mono<Void> process(T event, Function<T, Mono<Void>> handler) {
        UUID eventId = event.getEventId();
        if (eventId == null) {
            log.warn("Event missing eventId, processing without idempotency check: {}", event.getEventType());
            return handler.apply(event);
        }

        return processedEventRepository.findByEventId(eventId)
                .flatMap(existing -> {
                    log.info("Duplicate event detected, skipping: eventId={}, eventType={}", eventId, event.getEventType());
                    return Mono.<Void>empty();
                })
                .switchIfEmpty(Mono.defer(() -> handler.apply(event)
                        .then(processedEventRepository.save(eventId, java.time.LocalDateTime.now()).then())
                ))
                .as(transactionalOperator::transactional);
    }
}