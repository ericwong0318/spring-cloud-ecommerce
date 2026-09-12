package com.example.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Product variant data transfer object
 */
public record ProductVariantDto(
    @Schema(description = "Unique identifier", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    String id,

    @Schema(description = "Product ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    String productId,

    @NotBlank(message = "SKU code is required")
    @Size(max = 100)
    @Schema(description = "SKU code", example = "LAPTOP-13-SILVER", requiredMode = Schema.RequiredMode.REQUIRED)
    String skuCode,

    @Schema(description = "Variant attributes (e.g., size, color)", example = "{\"size\":\"13\",\"color\":\"silver\"}")
    Map<String, String> attributes,

    @NotNull(message = "Price is required")
    @Schema(description = "Variant price", example = "999.99", requiredMode = Schema.RequiredMode.REQUIRED)
    BigDecimal price,

    @Schema(description = "Linked inventory ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    String inventoryId
) {}