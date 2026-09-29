package com.example.common.event;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ReactiveOutboxEventRepository<T extends OutboxEvent> {

    Flux<T> findUnpublishedEvents();

    Flux<T> findUnpublishedEventsWithRetryLimit(int maxRetries);

    Mono<Boolean> existsById(UUID id);

    Mono<T> save(T event);
}