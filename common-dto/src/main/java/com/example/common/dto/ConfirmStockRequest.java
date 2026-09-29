package com.example.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Stock confirmation request")
public record ConfirmStockRequest(
    @NotNull(message = "Variant ID is required")
    @Schema(description = "Product variant ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    Long variantId,

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Schema(description = "Quantity to confirm", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    Integer quantity
) {}