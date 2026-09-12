package com.example.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Order data transfer object")
public record OrderDto(
    @Schema(description = "Unique identifier", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    Long id,

    @NotBlank(message = "Customer ID is required")
    @Size(max = 255)
    @Schema(description = "Customer identifier", example = "CUST-001", requiredMode = Schema.RequiredMode.REQUIRED)
    String customerId,

    @Schema(description = "Customer email (populated on read)", example = "customer@example.com", accessMode = Schema.AccessMode.READ_ONLY)
    String customerEmail,

    @NotNull(message = "Status is required")
    @Schema(description = "Order status", example = "PENDING", requiredMode = Schema.RequiredMode.REQUIRED)
    OrderStatus status,

    @NotNull(message = "Total amount is required")
    @Schema(description = "Total order amount", example = "1999.98", requiredMode = Schema.RequiredMode.REQUIRED)
    BigDecimal totalAmount,

    @Schema(description = "Order items", requiredMode = Schema.RequiredMode.REQUIRED)
    List<OrderItemDto> items,

    @Schema(description = "Order shipments", accessMode = Schema.AccessMode.READ_ONLY)
    List<ShipmentDto> shipments,

    @Schema(description = "Creation timestamp", example = "2024-01-15T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    LocalDateTime createdAt,

    @Schema(description = "Last update timestamp", example = "2024-01-15T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    LocalDateTime updatedAt
) {
    public enum OrderStatus {
        PENDING, RESERVED, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
    }

    public OrderDto withStatus(OrderStatus status) {
        return new OrderDto(id, customerId, customerEmail, status, totalAmount, items, shipments, createdAt, updatedAt);
    }

    public static OrderDtoBuilder builder() {
        return new OrderDtoBuilder();
    }

    public static class OrderDtoBuilder {
        private Long id;
        private String customerId;
        private String customerEmail;
        private OrderStatus status;
        private BigDecimal totalAmount;
        private List<OrderItemDto> items;
        private List<ShipmentDto> shipments;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public OrderDtoBuilder id(Long id) { this.id = id; return this; }
        public OrderDtoBuilder customerId(String customerId) { this.customerId = customerId; return this; }
        public OrderDtoBuilder customerEmail(String customerEmail) { this.customerEmail = customerEmail; return this; }
        public OrderDtoBuilder status(OrderStatus status) { this.status = status; return this; }
        public OrderDtoBuilder totalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; return this; }
        public OrderDtoBuilder items(List<OrderItemDto> items) { this.items = items; return this; }
        public OrderDtoBuilder shipments(List<ShipmentDto> shipments) { this.shipments = shipments; return this; }
        public OrderDtoBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public OrderDtoBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public OrderDto build() {
            return new OrderDto(id, customerId, customerEmail, status, totalAmount, items, shipments, createdAt, updatedAt);
        }
    }
}