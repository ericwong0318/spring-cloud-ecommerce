package com.example.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Shipment data transfer object
 */
public record ShipmentDto(
    @Schema(description = "Unique identifier", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    Long id,

    @Schema(description = "Order ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    Long orderId,

    @NotBlank(message = "Tracking number is required")
    @Size(max = 100)
    @Schema(description = "Tracking number", example = "1Z999AA10123456784", requiredMode = Schema.RequiredMode.REQUIRED)
    String trackingNumber,

    @Size(max = 50)
    @Schema(description = "Carrier name", example = "UPS")
    String carrier,

    @Schema(description = "Shipment status", example = "CREATED", accessMode = Schema.AccessMode.READ_ONLY)
    ShipmentStatus status,

    @Schema(description = "Shipped timestamp", example = "2024-01-15T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    LocalDateTime shippedAt,

    @Schema(description = "Delivered timestamp", example = "2024-01-20T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    LocalDateTime deliveredAt,

    @Schema(description = "Shipment items")
    List<ShipmentItemDto> items,

    @Schema(description = "Creation timestamp", example = "2024-01-15T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    LocalDateTime createdAt,

    @Schema(description = "Last update timestamp", example = "2024-01-15T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    LocalDateTime updatedAt
) {
    public enum ShipmentStatus {
        CREATED, SHIPPED, IN_TRANSIT, DELIVERED, EXCEPTION
    }
}