package com.example.order.outbox;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface R2dbcOutboxEventRepository extends R2dbcRepository<R2dbcOutboxEvent, UUID> {

    @Query("SELECT * FROM event_outbox WHERE published_at IS NULL ORDER BY created_at ASC")
    Flux<R2dbcOutboxEvent> findUnpublishedEvents();

    @Query("SELECT * FROM event_outbox WHERE published_at IS NULL AND retry_count < :maxRetries ORDER BY created_at ASC")
    Flux<R2dbcOutboxEvent> findUnpublishedEventsWithRetryLimit(int maxRetries);

    Mono<Boolean> existsById(UUID id);
}