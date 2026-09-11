package com.example.order.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Table("order_items")
public class OrderItem {

    @Id
    private Long id;

    @Column("order_id")
    private Long orderId;

    @Column("product_id")
    private Long productId;

    @Column("variant_id")
    private Long variantId;

    @Column("sku_code")
    private String skuCode;

    private String productName;

    @Column("quantity_ordered")
    private Integer quantityOrdered;

    @Column("quantity_shipped")
    private Integer quantityShipped;

    @Column("reserved_at")
    private LocalDateTime reservedAt;

    @Column("unit_price")
    private BigDecimal unitPrice;

    @Column("status")
    private OrderItemStatus status;

    public enum OrderItemStatus {
        PENDING, RESERVED, SHIPPED, BACKORDERED, CANCELLED
    }

    public OrderItem() {
    }

    public OrderItem(Long id, Long orderId, Long productId, Long variantId, String skuCode,
                     String productName, Integer quantityOrdered, Integer quantityShipped,
                     LocalDateTime reservedAt, BigDecimal unitPrice, OrderItemStatus status) {
        this.id = id;
        this.orderId = orderId;
        this.productId = productId;
        this.variantId = variantId;
        this.skuCode = skuCode;
        this.productName = productName;
        this.quantityOrdered = quantityOrdered;
        this.quantityShipped = quantityShipped;
        this.reservedAt = reservedAt;
        this.unitPrice = unitPrice;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
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

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getQuantityOrdered() {
        return quantityOrdered;
    }

    public void setQuantityOrdered(Integer quantityOrdered) {
        this.quantityOrdered = quantityOrdered;
    }

    public Integer getQuantityShipped() {
        return quantityShipped;
    }

    public void setQuantityShipped(Integer quantityShipped) {
        this.quantityShipped = quantityShipped;
    }

    public LocalDateTime getReservedAt() {
        return reservedAt;
    }

    public void setReservedAt(LocalDateTime reservedAt) {
        this.reservedAt = reservedAt;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public OrderItemStatus getStatus() {
        return status;
    }

    public void setStatus(OrderItemStatus status) {
        this.status = status;
    }

    public Integer getRemainingQuantity() {
        return quantityOrdered - quantityShipped;
    }

    public boolean isFullyShipped() {
        return quantityShipped >= quantityOrdered;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderItem orderItem = (OrderItem) o;
        return Objects.equals(id, orderItem.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "OrderItem{" +
                "id=" + id +
                ", status=" + status +
                ", reservedAt=" + reservedAt +
                '}';
    }
}
