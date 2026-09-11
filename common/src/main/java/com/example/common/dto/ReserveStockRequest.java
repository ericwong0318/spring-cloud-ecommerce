package com.example.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Stock reservation request")
public class ReserveStockRequest {

    @NotNull(message = "Variant ID is required")
    @Schema(description = "Product variant ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long variantId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Schema(description = "Quantity to reserve", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantity;

    @Schema(description = "Order item ID for tracking", example = "100", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Long orderItemId;

    public ReserveStockRequest() {
    }

    public ReserveStockRequest(Long variantId, Integer quantity, Long orderItemId) {
        this.variantId = variantId;
        this.quantity = quantity;
        this.orderItemId = orderItemId;
    }

    public Long getVariantId() {
        return variantId;
    }

    public void setVariantId(Long variantId) {
        this.variantId = variantId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Long getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(Long orderItemId) {
        this.orderItemId = orderItemId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long variantId;
        private Integer quantity;
        private Long orderItemId;

        public Builder variantId(Long variantId) {
            this.variantId = variantId;
            return this;
        }

        public Builder quantity(Integer quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder orderItemId(Long orderItemId) {
            this.orderItemId = orderItemId;
            return this;
        }

        public ReserveStockRequest build() {
            return new ReserveStockRequest(variantId, quantity, orderItemId);
        }
    }
}