package com.example.payment.repository;

import com.example.payment.domain.ProcessedEvent;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface ProcessedEventRepository extends R2dbcRepository<ProcessedEvent, UUID> {

    @Query("SELECT * FROM processed_events WHERE event_id = :eventId")
    Mono<ProcessedEvent> findByEventId(UUID eventId);

    @Query("INSERT INTO processed_events (event_id, processed_at) VALUES (:eventId, :processedAt)")
    Mono<Integer> save(UUID eventId, java.time.LocalDateTime processedAt);
}
