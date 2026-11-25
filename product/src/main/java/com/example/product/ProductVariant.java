package com.example.product;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Table("product_variants")
public class ProductVariant {

    @Id
    private String id;

    @Column("product_id")
    private String productId;

    @Column("sku_code")
    private String skuCode;

    @Column("attributes")
    private String attributesJson;

    @Column("variant_price")
    private BigDecimal price;

    @Column("inventory_id")
    private String inventoryId;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public ProductVariant() {
        this.attributesJson = "{}";
    }

    public ProductVariant(String skuCode, Map<String, String> attributes, BigDecimal price, String inventoryId) {
        this.skuCode = skuCode;
        setAttributes(attributes != null ? attributes : new HashMap<>());
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
        if (attributesJson == null || attributesJson.isBlank()) {
            return new HashMap<>();
        }
        try {
            return OBJECT_MAPPER.readValue(attributesJson,
                    OBJECT_MAPPER.getTypeFactory().constructMapType(HashMap.class, String.class, String.class));
        } catch (JsonProcessingException e) {
            return new HashMap<>();
        }
    }

    public void setAttributes(Map<String, String> attributes) {
        try {
            this.attributesJson = OBJECT_MAPPER.writeValueAsString(attributes != null ? attributes : new HashMap<>());
        } catch (JsonProcessingException e) {
            this.attributesJson = "{}";
        }
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
