package com.example.common.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class InventoryEvent implements BaseEvent {

    private String eventType;
    private UUID eventId;
    private Long variantId;
    private Long productId;
    private String productName;
    private Integer quantity;
    private Integer reservedQuantity;
    private Integer backorderedQuantity;
    private Integer reserved;
    private Integer backordered;
    private Integer availableQuantity;
    private BigDecimal costPrice;
    private LocalDateTime timestamp;

    public InventoryEvent() {
    }

    public InventoryEvent(String eventType, UUID eventId, Long variantId, Long productId, String productName,
                           Integer quantity, Integer reservedQuantity, Integer backorderedQuantity,
                           Integer availableQuantity, BigDecimal costPrice, LocalDateTime timestamp) {
        this.eventType = eventType;
        this.eventId = eventId;
        this.variantId = variantId;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.reservedQuantity = reservedQuantity;
        this.backorderedQuantity = backorderedQuantity;
        this.availableQuantity = availableQuantity;
        this.costPrice = costPrice;
        this.timestamp = timestamp;
    }

    public enum EventType {
        CREATED, UPDATED, DELETED,
        RESERVED, RELEASED, CONFIRMED,
        LOW_STOCK, STOCK_ADDED
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

    public Integer getBackorderedQuantity() {
        return backorderedQuantity;
    }

    public void setBackorderedQuantity(Integer backorderedQuantity) {
        this.backorderedQuantity = backorderedQuantity;
    }

    public Integer getReserved() {
        return reserved;
    }

    public void setReserved(Integer reserved) {
        this.reserved = reserved;
    }

    public Integer getBackordered() {
        return backordered;
    }

    public void setBackordered(Integer backordered) {
        this.backordered = backordered;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public static InventoryEvent created(Long productId, String productName) {
        InventoryEvent event = new InventoryEvent();
        event.setEventType(EventType.CREATED.name());
        event.setEventId(UUID.randomUUID());
        event.setProductId(productId);
        event.setProductName(productName);
        event.setQuantity(0);
        event.setReservedQuantity(0);
        event.setAvailableQuantity(0);
        event.setTimestamp(LocalDateTime.now());
        return event;
    }

    public static InventoryEvent reserved(Long variantId, Long productId, Integer reservedQuantity, Integer backorderedQuantity) {
        InventoryEvent event = new InventoryEvent();
        event.setEventType(EventType.RESERVED.name());
        event.setEventId(UUID.randomUUID());
        event.setVariantId(variantId);
        event.setProductId(productId);
        event.setReservedQuantity(reservedQuantity);
        event.setBackorderedQuantity(backorderedQuantity);
        event.setReserved(reservedQuantity);
        event.setBackordered(backorderedQuantity);
        event.setTimestamp(LocalDateTime.now());
        return event;
    }

    public static InventoryEvent released(Long variantId, Long productId, Integer reservedQuantity, Integer availableQuantity) {
        InventoryEvent event = new InventoryEvent();
        event.setEventType(EventType.RELEASED.name());
        event.setEventId(UUID.randomUUID());
        event.setVariantId(variantId);
        event.setProductId(productId);
        event.setReservedQuantity(reservedQuantity);
        event.setAvailableQuantity(availableQuantity);
        event.setTimestamp(LocalDateTime.now());
        return event;
    }

    public static InventoryEvent confirmed(Long variantId, Long productId, Integer quantity, Integer availableQuantity) {
        InventoryEvent event = new InventoryEvent();
        event.setEventType(EventType.CONFIRMED.name());
        event.setEventId(UUID.randomUUID());
        event.setVariantId(variantId);
        event.setProductId(productId);
        event.setQuantity(quantity);
        event.setAvailableQuantity(availableQuantity);
        event.setTimestamp(LocalDateTime.now());
        return event;
    }

    public static InventoryEvent lowStock(Long variantId, Long productId, Integer availableQuantity, Integer reorderLevel) {
        InventoryEvent event = new InventoryEvent();
        event.setEventType(EventType.LOW_STOCK.name());
        event.setEventId(UUID.randomUUID());
        event.setVariantId(variantId);
        event.setProductId(productId);
        event.setAvailableQuantity(availableQuantity);
        event.setTimestamp(LocalDateTime.now());
        return event;
    }

    public static InventoryEvent stockAdded(Long variantId, Long productId, Integer quantity, Integer availableQuantity) {
        InventoryEvent event = new InventoryEvent();
        event.setEventType(EventType.STOCK_ADDED.name());
        event.setEventId(UUID.randomUUID());
        event.setVariantId(variantId);
        event.setProductId(productId);
        event.setQuantity(quantity);
        event.setAvailableQuantity(availableQuantity);
        event.setTimestamp(LocalDateTime.now());
        return event;
    }

    public static InventoryEvent updated(Long variantId, Long productId, String productName, Integer quantity, Integer reservedQuantity, Integer availableQuantity, BigDecimal costPrice) {
        InventoryEvent event = new InventoryEvent();
        event.setEventType(EventType.UPDATED.name());
        event.setEventId(UUID.randomUUID());
        event.setVariantId(variantId);
        event.setProductId(productId);
        event.setProductName(productName);
        event.setQuantity(quantity);
        event.setReservedQuantity(reservedQuantity);
        event.setAvailableQuantity(availableQuantity);
        event.setCostPrice(costPrice);
        event.setTimestamp(LocalDateTime.now());
        return event;
    }

    public static InventoryEvent deleted(Long variantId, Long productId) {
        InventoryEvent event = new InventoryEvent();
        event.setEventType(EventType.DELETED.name());
        event.setEventId(UUID.randomUUID());
        event.setVariantId(variantId);
        event.setProductId(productId);
        event.setTimestamp(LocalDateTime.now());
        return event;
    }
}