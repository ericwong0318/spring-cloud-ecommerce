package com.example.common.event;

import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

public interface CommonProcessedEventRepository {
    
    boolean existsByEventId(UUID eventId);
    
    void saveBlocking(ProcessedEvent event);
    
    Mono<Boolean> existsByEventIdReactive(UUID eventId);
    
    Mono<Integer> save(UUID eventId, LocalDateTime processedAt);
}
