package com.example.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Shipment data transfer object")
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

    @NotNull(message = "Status is required")
    @Schema(description = "Shipment status", example = "CREATED", requiredMode = Schema.RequiredMode.REQUIRED)
    private ShipmentStatus status;

    @Schema(description = "Shipped timestamp", example = "2024-01-15T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime shippedAt;

    @Schema(description = "Delivered timestamp", example = "2024-01-20T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime deliveredAt;

    @Schema(description = "Shipment items", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ShipmentItemDto> items;

    @Schema(description = "Creation timestamp", example = "2024-01-15T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp", example = "2024-01-15T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;

    public enum ShipmentStatus {
        CREATED, SHIPPED, IN_TRANSIT, DELIVERED, EXCEPTION
    }
}