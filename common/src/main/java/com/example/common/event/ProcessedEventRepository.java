package com.example.common.event;

import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProcessedEventRepository {
    
    boolean existsByEventId(UUID eventId);
    
    void save(ProcessedEvent event);
}
