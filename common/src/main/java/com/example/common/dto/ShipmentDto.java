package com.example.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Shipment data transfer object
 */
public class ShipmentDto {

    @Schema(description = "Unique identifier", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "Order ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long orderId;

    @NotBlank(message = "Tracking number is required")
    @Size(max = 100)
    @Schema(description = "Tracking number", example = "1Z999AA10123456784", requiredMode = Schema.RequiredMode.REQUIRED)
    private String trackingNumber;

    @Size(max = 50)
    @Schema(description = "Carrier name", example = "UPS")
    private String carrier;

    @Schema(description = "Shipment status", example = "CREATED", accessMode = Schema.AccessMode.READ_ONLY)
    private ShipmentStatus status;

    @Schema(description = "Shipped timestamp", example = "2024-01-15T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime shippedAt;

    @Schema(description = "Delivered timestamp", example = "2024-01-20T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime deliveredAt;

    @Schema(description = "Shipment items")
    private List<ShipmentItemDto> items;

    @Schema(description = "Creation timestamp", example = "2024-01-15T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp", example = "2024-01-15T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;

    public ShipmentDto() {
    }

    public ShipmentDto(Long id, Long orderId, String trackingNumber, String carrier, ShipmentStatus status,
                        LocalDateTime shippedAt, LocalDateTime deliveredAt, List<ShipmentItemDto> items,
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.orderId = orderId;
        this.trackingNumber = trackingNumber;
        this.carrier = carrier;
        this.status = status;
        this.shippedAt = shippedAt;
        this.deliveredAt = deliveredAt;
        this.items = items;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getCarrier() {
        return carrier;
    }

    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public void setStatus(ShipmentStatus status) {
        this.status = status;
    }

    public LocalDateTime getShippedAt() {
        return shippedAt;
    }

    public void setShippedAt(LocalDateTime shippedAt) {
        this.shippedAt = shippedAt;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public List<ShipmentItemDto> getItems() {
        return items;
    }

    public void setItems(List<ShipmentItemDto> items) {
        this.items = items;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public enum ShipmentStatus {
        CREATED, SHIPPED, IN_TRANSIT, DELIVERED, EXCEPTION
    }
}