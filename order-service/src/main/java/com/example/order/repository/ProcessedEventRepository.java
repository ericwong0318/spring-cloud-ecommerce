package com.example.order.repository;

import com.example.common.event.ProcessedEvent;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface ProcessedEventRepository extends R2dbcRepository<com.example.order.domain.ProcessedEvent, UUID> {

    @Query("SELECT * FROM processed_events WHERE event_id = :eventId")
    Mono<com.example.order.domain.ProcessedEvent> findByEventId(UUID eventId);

    @Query("INSERT INTO processed_events (event_id, processed_at) VALUES (:eventId, :processedAt)")
    Mono<Integer> save(UUID eventId, java.time.LocalDateTime processedAt);

    @Query("SELECT EXISTS(SELECT 1 FROM processed_events WHERE event_id = :eventId)")
    Mono<Boolean> existsByEventId(UUID eventId);

    @Query("SELECT COUNT(*) FROM processed_events")
    Mono<Long> count();
    
    // Implement common interface method
    default Mono<Void> save(ProcessedEvent event) {
        return save(event.getEventId(), event.getProcessedAt()).then();
    }
}
