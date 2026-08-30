package com.example.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Product variant data transfer object")
public class ProductVariantDto {

    @Schema(description = "Unique identifier", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "Product ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long productId;

    @NotBlank(message = "SKU code is required")
    @Size(max = 100)
    @Schema(description = "SKU code", example = "LAPTOP-13-SILVER", requiredMode = Schema.RequiredMode.REQUIRED)
    private String skuCode;

    @Schema(description = "Variant attributes (e.g., size, color)", example = "{\"size\":\"13\",\"color\":\"silver\"}")
    private Map<String, String> attributes;

    @NotNull(message = "Price is required")
    @Schema(description = "Variant price", example = "999.99", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal price;

    @Schema(description = "Linked inventory ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long inventoryId;
}