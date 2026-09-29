package com.example.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Shipment item data transfer object
 */
public record ShipmentItemDto(
    @Schema(description = "Unique identifier", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    Long id,

    @NotNull(message = "Order item ID is required")
    @Schema(description = "Order item ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    Long orderItemId,

    @Schema(description = "Product name (populated on read)", example = "Laptop", accessMode = Schema.AccessMode.READ_ONLY)
    String productName,

    @NotNull(message = "Quantity is required")
    @jakarta.validation.constraints.Min(value = 1, message = "Quantity must be at least 1")
    @Schema(description = "Quantity shipped", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    Integer quantity
) {}