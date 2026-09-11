package com.example.product;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ProductVariant {

    private String skuCode;
    private Map<String, String> attributes = new HashMap<>();
    private BigDecimal price;
    private String inventoryId;

    public ProductVariant() {
    }

    public ProductVariant(String skuCode, Map<String, String> attributes, BigDecimal price, String inventoryId) {
        this.skuCode = skuCode;
        this.attributes = attributes;
        this.price = price;
        this.inventoryId = inventoryId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductVariant that = (ProductVariant) o;
        return Objects.equals(skuCode, that.skuCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(skuCode);
    }
}
