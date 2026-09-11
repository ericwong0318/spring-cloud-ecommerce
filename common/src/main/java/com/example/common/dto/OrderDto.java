package com.example.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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

    public OrderDto() {
    }

    public OrderDto(Long id, String customerId, String customerEmail, OrderStatus status,
                    BigDecimal totalAmount, List<OrderItemDto> items, List<ShipmentDto> shipments,
                    LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.customerId = customerId;
        this.customerEmail = customerEmail;
        this.status = status;
        this.totalAmount = totalAmount;
        this.items = items;
        this.shipments = shipments;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static OrderDtoBuilder builder() {
        return new OrderDtoBuilder();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public List<OrderItemDto> getItems() {
        return items;
    }

    public void setItems(List<OrderItemDto> items) {
        this.items = items;
    }

    public List<ShipmentDto> getShipments() {
        return shipments;
    }

    public void setShipments(List<ShipmentDto> shipments) {
        this.shipments = shipments;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public enum OrderStatus {
        PENDING, RESERVED, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
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

        public OrderDtoBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public OrderDtoBuilder customerId(String customerId) {
            this.customerId = customerId;
            return this;
        }

        public OrderDtoBuilder customerEmail(String customerEmail) {
            this.customerEmail = customerEmail;
            return this;
        }

        public OrderDtoBuilder status(OrderStatus status) {
            this.status = status;
            return this;
        }

        public OrderDtoBuilder totalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public OrderDtoBuilder items(List<OrderItemDto> items) {
            this.items = items;
            return this;
        }

        public OrderDtoBuilder shipments(List<ShipmentDto> shipments) {
            this.shipments = shipments;
            return this;
        }

        public OrderDtoBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public OrderDtoBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public OrderDto build() {
            return new OrderDto(id, customerId, customerEmail, status, totalAmount, items, shipments, createdAt, updatedAt);
        }
    }
}
