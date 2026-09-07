package com.example.payment.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProcessedEvent(
        UUID eventId,
        LocalDateTime processedAt
) {
}