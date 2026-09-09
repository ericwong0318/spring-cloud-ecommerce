package com.example.payment.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Table("processed_events")
public record ProcessedEvent(
        @Id UUID eventId,
        LocalDateTime processedAt
) {
}