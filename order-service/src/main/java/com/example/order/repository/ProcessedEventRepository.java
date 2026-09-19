package com.example.order.repository;

import com.example.common.event.CommonProcessedEventRepository;
import com.example.common.event.ProcessedEvent;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface ProcessedEventRepository extends R2dbcRepository<com.example.order.domain.ProcessedEvent, UUID>, CommonProcessedEventRepository {

    @Query("SELECT * FROM processed_events WHERE event_id = :eventId")
    Mono<com.example.order.domain.ProcessedEvent> findByEventId(UUID eventId);

    @Query("INSERT INTO processed_events (event_id, processed_at) VALUES (:eventId, :processedAt)")
    Mono<Integer> save(UUID eventId, LocalDateTime processedAt);

    @Override
    default boolean existsByEventId(UUID eventId) {
        return findByEventId(eventId).map(e -> true).defaultIfEmpty(false).block();
    }

    @Override
    default void saveBlocking(ProcessedEvent event) {
        save(event.getEventId(), event.getProcessedAt()).block();
    }

    @Override
    default Mono<Boolean> existsByEventIdReactive(UUID eventId) {
        return findByEventId(eventId).map(e -> true).defaultIfEmpty(false);
    }
}
