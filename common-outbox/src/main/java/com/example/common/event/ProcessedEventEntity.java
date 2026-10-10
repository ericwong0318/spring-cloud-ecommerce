package com.example.common.event;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "processed_events")
public class ProcessedEventEntity {

    @Id
    private UUID eventId;

    private LocalDateTime processedAt;

    public ProcessedEventEntity() {}

    public ProcessedEventEntity(UUID eventId, LocalDateTime processedAt) {
        this.eventId = eventId;
        this.processedAt = processedAt;
    }

    public static ProcessedEventEntity of(UUID eventId) {
        return new ProcessedEventEntity(eventId, LocalDateTime.now());
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
}