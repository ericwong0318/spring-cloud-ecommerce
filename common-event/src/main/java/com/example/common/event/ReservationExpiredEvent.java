package com.example.common.event;

import java.time.LocalDateTime;
import java.util.UUID;

public class ReservationExpiredEvent implements BaseEvent {

    private String eventType;
    private UUID eventId;
    private Long orderItemId;
    private Long variantId;
    private Integer quantityReleased;
    private LocalDateTime reservationExpiresAt;

    public ReservationExpiredEvent() {
    }

    public ReservationExpiredEvent(String eventType, UUID eventId, Long orderItemId, Long variantId,
                                   Integer quantityReleased, LocalDateTime reservationExpiresAt) {
        this.eventType = eventType;
        this.eventId = eventId;
        this.orderItemId = orderItemId;
        this.variantId = variantId;
        this.quantityReleased = quantityReleased;
        this.reservationExpiresAt = reservationExpiresAt;
    }

    public enum EventType {
        EXPIRED
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public Long getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(Long orderItemId) {
        this.orderItemId = orderItemId;
    }

    public Long getVariantId() {
        return variantId;
    }

    public void setVariantId(Long variantId) {
        this.variantId = variantId;
    }

    public Integer getQuantityReleased() {
        return quantityReleased;
    }

    public void setQuantityReleased(Integer quantityReleased) {
        this.quantityReleased = quantityReleased;
    }

    public LocalDateTime getReservationExpiresAt() {
        return reservationExpiresAt;
    }

    public void setReservationExpiresAt(LocalDateTime reservationExpiresAt) {
        this.reservationExpiresAt = reservationExpiresAt;
    }

    public static ReservationExpiredEvent expired(Long orderItemId, Long variantId, Integer quantityReleased, LocalDateTime reservationExpiresAt) {
        ReservationExpiredEvent event = new ReservationExpiredEvent();
        event.setEventType(EventType.EXPIRED.name());
        event.setEventId(UUID.randomUUID());
        event.setOrderItemId(orderItemId);
        event.setVariantId(variantId);
        event.setQuantityReleased(quantityReleased);
        event.setReservationExpiresAt(reservationExpiresAt);
        return event;
    }
}
