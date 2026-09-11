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
    private Long productId;
    @JsonProperty("productName")
    private String productName;
    @JsonProperty("price")
    private BigDecimal price;
    @JsonProperty("categoryId")
    private Long categoryId;
    @JsonProperty("variantId")
    private Long variantId;
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

    public ProductEvent(String eventType, UUID eventId, Long productId, String productName, BigDecimal price, Long categoryId, Long variantId, String skuCode, LocalDateTime timestamp) {
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

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getVariantId() {
        return variantId;
    }

    public void setVariantId(Long variantId) {
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
