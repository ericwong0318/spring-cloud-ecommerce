package com.example.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
@Schema(description = "Order item data transfer object")
public class OrderItemDto {

    @Schema(description = "Unique identifier", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotNull(message = "Product ID is required")
    @Schema(description = "Product ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long productId;

    @Schema(description = "Variant ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long variantId;

    @Schema(description = "SKU code", example = "LAPTOP-13-SILVER", accessMode = Schema.AccessMode.READ_ONLY)
    private String skuCode;

    @Schema(description = "Product name (populated on read)", example = "Laptop", accessMode = Schema.AccessMode.READ_ONLY)
    private String productName;

    @NotNull(message = "Quantity is required")
    @jakarta.validation.constraints.Min(value = 1, message = "Quantity must be at least 1")
    @Schema(description = "Quantity ordered", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantity;

    @Schema(description = "Quantity shipped", example = "0", accessMode = Schema.AccessMode.READ_ONLY)
    private Integer quantityShipped;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Schema(description = "Unit price at time of order", example = "999.99", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal price;

    @Schema(description = "Order item status", example = "PENDING", accessMode = Schema.AccessMode.READ_ONLY)
    private OrderItemStatus status;

    @Schema(description = "Reservation timestamp", example = "2024-01-15T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime reservedAt;

    public enum OrderItemStatus {
        PENDING, RESERVED, SHIPPED, BACKORDERED, CANCELLED
    }
}