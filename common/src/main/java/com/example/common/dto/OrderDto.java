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
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Order data transfer object")
public class OrderDto {

    @Schema(description = "Unique identifier", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank(message = "Customer ID is required")
    @Size(max = 255)
    @Schema(description = "Customer identifier", example = "CUST-001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String customerId;

    @Schema(description = "Customer email (populated on read)", example = "customer@example.com", accessMode = Schema.AccessMode.READ_ONLY)
    private String customerEmail;

    @NotNull(message = "Status is required")
    @Schema(description = "Order status", example = "PENDING", requiredMode = Schema.RequiredMode.REQUIRED)
    private OrderStatus status;

    @NotNull(message = "Total amount is required")
    @Schema(description = "Total order amount", example = "1999.98", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal totalAmount;

    @Schema(description = "Order items", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<OrderItemDto> items;

    @Schema(description = "Order shipments", accessMode = Schema.AccessMode.READ_ONLY)
    private List<ShipmentDto> shipments;

    @Schema(description = "Creation timestamp", example = "2024-01-15T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp", example = "2024-01-15T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;

    public enum OrderStatus {
        PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
    }
}