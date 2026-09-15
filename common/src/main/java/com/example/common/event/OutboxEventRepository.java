package com.example.common.event;

import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository {
    
    List<OutboxEvent> findUnpublishedEvents();
    
    List<OutboxEvent> findUnpublishedEventsWithRetryLimit(int maxRetries);
    
    boolean existsById(UUID id);
    
    void save(OutboxEvent event);
}
