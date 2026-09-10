package com.example.order.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Table("processed_events")
public record ProcessedEvent(
        @Id UUID eventId,
        LocalDateTime processedAt
) {
    public static ProcessedEvent of(UUID eventId) {
        return new ProcessedEvent(eventId, LocalDateTime.now());
    }
}