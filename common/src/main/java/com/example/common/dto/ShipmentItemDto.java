package com.example.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Shipment item data transfer object
 */
public class ShipmentItemDto {

    @Schema(description = "Unique identifier", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotNull(message = "Order item ID is required")
    @Schema(description = "Order item ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long orderItemId;

    @Schema(description = "Product name (populated on read)", example = "Laptop", accessMode = Schema.AccessMode.READ_ONLY)
    private String productName;

    @NotNull(message = "Quantity is required")
    @jakarta.validation.constraints.Min(value = 1, message = "Quantity must be at least 1")
    @Schema(description = "Quantity shipped", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantity;

    public ShipmentItemDto() {
    }

    public ShipmentItemDto(Long id, Long orderItemId, String productName, Integer quantity) {
        this.id = id;
        this.orderItemId = orderItemId;
        this.productName = productName;
        this.quantity = quantity;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(Long orderItemId) {
        this.orderItemId = orderItemId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}