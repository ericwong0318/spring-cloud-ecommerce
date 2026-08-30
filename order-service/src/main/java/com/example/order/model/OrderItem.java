package com.example.order.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "variant_id")
    private Long variantId;

    @Column(name = "sku_code", length = 100)
    private String skuCode;

    @Column(nullable = false)
    private String productName;

    @Column(name = "quantity_ordered", nullable = false)
    private Integer quantityOrdered;

    @Column(name = "quantity_shipped", nullable = false)
    @Builder.Default
    private Integer quantityShipped = 0;

    @Column(name = "reserved_at")
    private LocalDateTime reservedAt;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private OrderItemStatus status = OrderItemStatus.PENDING;

    public enum OrderItemStatus {
        PENDING, RESERVED, SHIPPED, BACKORDERED, CANCELLED
    }

    public Integer getRemainingQuantity() {
        return quantityOrdered - quantityShipped;
    }

    public boolean isFullyShipped() {
        return quantityShipped >= quantityOrdered;
    }
}