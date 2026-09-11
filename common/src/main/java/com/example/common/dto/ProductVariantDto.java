package com.example.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Product variant data transfer object
 */
public class ProductVariantDto {

    @Schema(description = "Unique identifier", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private String id;

    @Schema(description = "Product ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private String productId;

    @NotBlank(message = "SKU code is required")
    @Size(max = 100)
    @Schema(description = "SKU code", example = "LAPTOP-13-SILVER", requiredMode = Schema.RequiredMode.REQUIRED)
    private String skuCode;

    @Schema(description = "Variant attributes (e.g., size, color)", example = "{\"size\":\"13\",\"color\":\"silver\"}")
    private Map<String, String> attributes;

    @NotNull(message = "Price is required")
    @Schema(description = "Variant price", example = "999.99", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal price;

    @Schema(description = "Linked inventory ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private String inventoryId;

    public ProductVariantDto() {
    }

    public ProductVariantDto(String id, String productId, String skuCode, Map<String, String> attributes, BigDecimal price, String inventoryId) {
        this.id = id;
        this.productId = productId;
        this.skuCode = skuCode;
        this.attributes = attributes;
        this.price = price;
        this.inventoryId = inventoryId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public void setSkuCode(String skuCode) {
        this.skuCode = skuCode;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getInventoryId() {
        return inventoryId;
    }

    public void setInventoryId(String inventoryId) {
        this.inventoryId = inventoryId;
    }
}
