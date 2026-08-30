package com.example.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationExpiredEvent implements BaseEvent {

    private String eventType;
    private UUID eventId;
    private Long orderItemId;
    private Long variantId;
    private Integer quantityReleased;
    private LocalDateTime reservationExpiresAt;

    public enum EventType {
        EXPIRED
    }

    public static ReservationExpiredEvent expired(Long orderItemId, Long variantId, Integer quantityReleased, LocalDateTime reservationExpiresAt) {
        return ReservationExpiredEvent.builder()
                .eventType(EventType.EXPIRED.name())
                .eventId(UUID.randomUUID())
                .orderItemId(orderItemId)
                .variantId(variantId)
                .quantityReleased(quantityReleased)
                .reservationExpiresAt(reservationExpiresAt)
                .build();
    }
}