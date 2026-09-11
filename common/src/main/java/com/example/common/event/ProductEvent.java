package com.example.common.event;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class ProductEvent implements BaseEvent {

    @JsonProperty("eventType")
    private String eventType;
    @JsonProperty("eventId")
    private UUID eventId;
    @JsonProperty("productId")
    private String productId;
    @JsonProperty("productName")
    private String productName;
    @JsonProperty("price")
    private BigDecimal price;
    @JsonProperty("categoryId")
    private String categoryId;
    @JsonProperty("variantId")
    private String variantId;
    @JsonProperty("skuCode")
    private String skuCode;
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    public enum EventType {
        CREATED, UPDATED, DELETED,
        VARIANT_CREATED, VARIANT_UPDATED, VARIANT_DELETED,
        CATEGORY_CREATED, CATEGORY_UPDATED, CATEGORY_DELETED
    }

    public ProductEvent() {
    }

    public ProductEvent(String eventType, UUID eventId, String productId, String productName, BigDecimal price, String categoryId, String variantId, String skuCode, LocalDateTime timestamp) {
        this.eventType = eventType;
        this.eventId = eventId;
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.categoryId = categoryId;
        this.variantId = variantId;
        this.skuCode = skuCode;
        this.timestamp = timestamp;
    }

    public static ProductEvent created(Long productId, String productName, BigDecimal price, Long categoryId) {
        return created(String.valueOf(productId), productName, price, String.valueOf(categoryId));
    }

    public static ProductEvent created(String productId, String productName, BigDecimal price, String categoryId) {
        return new ProductEvent(
                EventType.CREATED.name(),
                UUID.randomUUID(),
                productId,
                productName,
                price,
                categoryId,
                null,
                null,
                LocalDateTime.now()
        );
    }

    public static ProductEvent updated(Long productId, String productName, BigDecimal price, Long categoryId) {
        return updated(String.valueOf(productId), productName, price, String.valueOf(categoryId));
    }

    public static ProductEvent updated(String productId, String productName, BigDecimal price, String categoryId) {
        return new ProductEvent(
                EventType.UPDATED.name(),
                UUID.randomUUID(),
                productId,
                productName,
                price,
                categoryId,
                null,
                null,
                LocalDateTime.now()
        );
    }

    public static ProductEvent deleted(Long productId) {
        return deleted(String.valueOf(productId));
    }

    public static ProductEvent deleted(String productId) {
        return new ProductEvent(
                EventType.DELETED.name(),
                UUID.randomUUID(),
                productId,
                null,
                null,
                null,
                null,
                null,
                LocalDateTime.now()
        );
    }

    public static ProductEvent variantCreated(Long productId, Long variantId, String skuCode, BigDecimal price, Long categoryId) {
        return variantCreated(String.valueOf(productId), String.valueOf(variantId), skuCode, price, String.valueOf(categoryId));
    }

    public static ProductEvent variantCreated(String productId, String variantId, String skuCode, BigDecimal price, String categoryId) {
        return new ProductEvent(
                EventType.VARIANT_CREATED.name(),
                UUID.randomUUID(),
                productId,
                null,
                price,
                categoryId,
                variantId,
                skuCode,
                LocalDateTime.now()
        );
    }

    public static ProductEvent variantUpdated(Long productId, Long variantId, String skuCode, BigDecimal price, Long categoryId) {
        return variantUpdated(String.valueOf(productId), String.valueOf(variantId), skuCode, price, String.valueOf(categoryId));
    }

    public static ProductEvent variantUpdated(String productId, String variantId, String skuCode, BigDecimal price, String categoryId) {
        return new ProductEvent(
                EventType.VARIANT_UPDATED.name(),
                UUID.randomUUID(),
                productId,
                null,
                price,
                categoryId,
                variantId,
                skuCode,
                LocalDateTime.now()
        );
    }

    public static ProductEvent variantDeleted(Long productId, Long variantId, String skuCode) {
        return variantDeleted(String.valueOf(productId), String.valueOf(variantId), skuCode);
    }

    public static ProductEvent variantDeleted(String productId, String variantId, String skuCode) {
        return new ProductEvent(
                EventType.VARIANT_DELETED.name(),
                UUID.randomUUID(),
                productId,
                null,
                null,
                null,
                variantId,
                skuCode,
                LocalDateTime.now()
        );
    }

    public static ProductEvent categoryCreated(Long categoryId, String categoryName, Long parentId) {
        return categoryCreated(String.valueOf(categoryId), categoryName, parentId != null ? String.valueOf(parentId) : null);
    }

    public static ProductEvent categoryCreated(String categoryId, String categoryName, String parentId) {
        return new ProductEvent(
                EventType.CATEGORY_CREATED.name(),
                UUID.randomUUID(),
                categoryId,
                categoryName,
                null,
                parentId,
                null,
                null,
                LocalDateTime.now()
        );
    }

    public static ProductEvent categoryUpdated(Long categoryId, String categoryName, Long parentId) {
        return categoryUpdated(String.valueOf(categoryId), categoryName, parentId != null ? String.valueOf(parentId) : null);
    }

    public static ProductEvent categoryUpdated(String categoryId, String categoryName, String parentId) {
        return new ProductEvent(
                EventType.CATEGORY_UPDATED.name(),
                UUID.randomUUID(),
                categoryId,
                categoryName,
                null,
                parentId,
                null,
                null,
                LocalDateTime.now()
        );
    }

    public static ProductEvent categoryDeleted(Long categoryId) {
        return categoryDeleted(String.valueOf(categoryId));
    }

    public static ProductEvent categoryDeleted(String categoryId) {
        return new ProductEvent(
                EventType.CATEGORY_DELETED.name(),
                UUID.randomUUID(),
                categoryId,
                null,
                null,
                null,
                null,
                null,
                LocalDateTime.now()
        );
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getVariantId() {
        return variantId;
    }

    public void setVariantId(String variantId) {
        this.variantId = variantId;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public void setSkuCode(String skuCode) {
        this.skuCode = skuCode;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}