package com.example.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

@Schema(description = "Create order item request")
public record CreateOrderItemRequest(
    @NotNull(message = "Product ID is required")
    @Schema(description = "Product ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    Long productId,

    @Schema(description = "Variant ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    Long variantId,

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Schema(description = "Quantity ordered", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    Integer quantity
) {
}