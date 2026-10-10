package com.example.common.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    
    @Query("SELECT e FROM OutboxEvent e WHERE e.publishedAt IS NULL")
    List<OutboxEvent> findUnpublishedEvents();
    
    @Query("SELECT e FROM OutboxEvent e WHERE e.publishedAt IS NULL AND e.retryCount <= :maxRetries")
    List<OutboxEvent> findUnpublishedEventsWithRetryLimit(@Param("maxRetries") int maxRetries);
    
    boolean existsById(UUID id);
    
    @Override
    OutboxEvent save(OutboxEvent event);
}
