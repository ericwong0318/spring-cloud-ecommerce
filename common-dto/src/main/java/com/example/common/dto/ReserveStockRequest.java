package com.example.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Stock reservation request")
public record ReserveStockRequest(
    @NotNull(message = "Variant ID is required")
    @Schema(description = "Product variant ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    Long variantId,

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Schema(description = "Quantity to reserve", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    Integer quantity,

    @Schema(description = "Order item ID for tracking", example = "100", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    Long orderItemId
) {}