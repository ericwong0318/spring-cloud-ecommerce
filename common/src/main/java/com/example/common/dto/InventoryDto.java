package com.example.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

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

    public InventoryDto() {
    }

    public InventoryDto(Long id, Long variantId, Long productId, String productName,
                        Integer quantity, Integer reservedQuantity, Integer availableQuantity,
                        Integer reorderLevel, BigDecimal costPrice, Boolean lowStock,
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.variantId = variantId;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.reservedQuantity = reservedQuantity;
        this.availableQuantity = availableQuantity;
        this.reorderLevel = reorderLevel;
        this.costPrice = costPrice;
        this.lowStock = lowStock;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVariantId() {
        return variantId;
    }

    public void setVariantId(Long variantId) {
        this.variantId = variantId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
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

    public Integer getReservedQuantity() {
        return reservedQuantity;
    }

    public void setReservedQuantity(Integer reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public Integer getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(Integer reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public Boolean getLowStock() {
        return lowStock;
    }

    public void setLowStock(Boolean lowStock) {
        this.lowStock = lowStock;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InventoryDto that = (InventoryDto) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "InventoryDto{" +
                "id=" + id +
                ", variantId=" + variantId +
                ", productId=" + productId +
                ", productName='" + productName + '\'' +
                ", quantity=" + quantity +
                ", reservedQuantity=" + reservedQuantity +
                ", availableQuantity=" + availableQuantity +
                ", reorderLevel=" + reorderLevel +
                ", costPrice=" + costPrice +
                ", lowStock=" + lowStock +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long variantId;
        private Long productId;
        private String productName;
        private Integer quantity;
        private Integer reservedQuantity;
        private Integer availableQuantity;
        private Integer reorderLevel = 10;
        private BigDecimal costPrice;
        private Boolean lowStock;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder variantId(Long variantId) {
            this.variantId = variantId;
            return this;
        }

        public Builder productId(Long productId) {
            this.productId = productId;
            return this;
        }

        public Builder productName(String productName) {
            this.productName = productName;
            return this;
        }

        public Builder quantity(Integer quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder reservedQuantity(Integer reservedQuantity) {
            this.reservedQuantity = reservedQuantity;
            return this;
        }

        public Builder availableQuantity(Integer availableQuantity) {
            this.availableQuantity = availableQuantity;
            return this;
        }

        public Builder reorderLevel(Integer reorderLevel) {
            this.reorderLevel = reorderLevel;
            return this;
        }

        public Builder costPrice(BigDecimal costPrice) {
            this.costPrice = costPrice;
            return this;
        }

        public Builder lowStock(Boolean lowStock) {
            this.lowStock = lowStock;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public InventoryDto build() {
            return new InventoryDto(id, variantId, productId, productName, quantity, reservedQuantity,
                    availableQuantity, reorderLevel, costPrice, lowStock, createdAt, updatedAt);
        }
    }
}