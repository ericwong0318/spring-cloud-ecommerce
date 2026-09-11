package com.example.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Inventory data transfer object")
public class InventoryDto {

    @Schema(description = "Unique identifier", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "Product variant ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long variantId;

    @NotNull(message = "Product ID is required")
    @Schema(description = "Product ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long productId;

    @Schema(description = "Product name", example = "Laptop", accessMode = Schema.AccessMode.READ_ONLY)
    private String productName;

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be negative")
    @Schema(description = "Total quantity in stock", example = "100", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantity;

    @Schema(description = "Reserved quantity for pending orders", example = "10", accessMode = Schema.AccessMode.READ_ONLY)
    private Integer reservedQuantity;

    @Schema(description = "Available quantity (quantity - reserved)", example = "90", accessMode = Schema.AccessMode.READ_ONLY)
    private Integer availableQuantity;

    @Min(value = 1, message = "Reorder level must be at least 1")
    @Schema(description = "Reorder level threshold", example = "10", defaultValue = "10")
    private Integer reorderLevel = 10;

    @DecimalMin(value = "0.0", inclusive = true, message = "Cost price cannot be negative")
    @Schema(description = "Cost price per unit", example = "500.00")
    private BigDecimal costPrice;

    @Schema(description = "Whether stock is low (available <= reorderLevel)", example = "false", accessMode = Schema.AccessMode.READ_ONLY)
    private Boolean lowStock;

    @Schema(description = "Creation timestamp", example = "2024-01-15T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp", example = "2024-01-15T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;
}