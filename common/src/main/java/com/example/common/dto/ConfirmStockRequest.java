package com.example.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Stock confirmation request")
public class ConfirmStockRequest {

    @NotNull(message = "Variant ID is required")
    @Schema(description = "Product variant ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long variantId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Schema(description = "Quantity to confirm", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantity;

    public ConfirmStockRequest() {
    }

    public ConfirmStockRequest(Long variantId, Integer quantity) {
        this.variantId = variantId;
        this.quantity = quantity;
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long variantId;
        private Integer quantity;

        public Builder variantId(Long variantId) {
            this.variantId = variantId;
            return this;
        }

        public Builder quantity(Integer quantity) {
            this.quantity = quantity;
            return this;
        }

        public ConfirmStockRequest build() {
            return new ConfirmStockRequest(variantId, quantity);
        }
    }
}