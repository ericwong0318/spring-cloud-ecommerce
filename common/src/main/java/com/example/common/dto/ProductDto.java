package com.example.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Product data transfer object")
public record ProductDto(
    @Schema(description = "Unique identifier", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    String id,

    @NotBlank(message = "Product name is required")
    @Size(max = 255, message = "Product name must not exceed 255 characters")
    @Schema(description = "Product name", example = "Laptop", requiredMode = Schema.RequiredMode.REQUIRED)
    String name,

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    @Schema(description = "Product description", example = "High-performance laptop")
    String description,

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Schema(description = "Product price", example = "999.99", requiredMode = Schema.RequiredMode.REQUIRED)
    BigDecimal price,

    @Schema(description = "Category ID", example = "1")
    String categoryId,

    @Schema(description = "Category name (populated on read)", example = "Electronics", accessMode = Schema.AccessMode.READ_ONLY)
    String categoryName,

    @Schema(description = "Product variants")
    List<ProductVariantDto> variants
) {
    public ProductDto {
        variants = variants != null ? new ArrayList<>(variants) : new ArrayList<>();
    }
}