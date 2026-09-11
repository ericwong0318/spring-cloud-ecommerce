package com.example.common.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query("SELECT e FROM OutboxEvent e WHERE e.publishedAt IS NULL ORDER BY e.createdAt ASC")
    List<OutboxEvent> findUnpublishedEvents();

    @Query("SELECT e FROM OutboxEvent e WHERE e.publishedAt IS NULL AND e.retryCount < :maxRetries ORDER BY e.createdAt ASC")
    List<OutboxEvent> findUnpublishedEventsWithRetryLimit(int maxRetries);

    boolean existsById(UUID id);
}
