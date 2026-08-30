package com.example.product;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "product_variants")
@Getter
@Setter
@NoArgsConstructor
@Builder
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "sku_code", nullable = false, unique = true, length = 100)
    private String skuCode;

    @Column(name = "attributes", columnDefinition = "JSONB")
    @Builder.Default
    private Map<String, String> attributes = new HashMap<>();

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "inventory_id")
    private Long inventoryId;

    public ProductVariant(Long id, Product product, String skuCode, Map<String, String> attributes, BigDecimal price, Long inventoryId) {
        this.id = id;
        this.product = product;
        this.skuCode = skuCode;
        this.attributes = attributes;
        this.price = price;
        this.inventoryId = inventoryId;
    }

    public void setProduct(Product product) {
        this.product = product;
    }
}