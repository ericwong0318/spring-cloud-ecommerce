package com.example.common.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductEvent implements BaseEvent {

    private String eventType;
    private UUID eventId;
    private Long productId;
    private String productName;
    private BigDecimal price;
    private Long categoryId;
    private Long variantId;
    private String skuCode;
    private LocalDateTime timestamp;

    public enum EventType {
        CREATED, UPDATED, DELETED,
        VARIANT_CREATED, VARIANT_UPDATED, VARIANT_DELETED,
        CATEGORY_CREATED, CATEGORY_UPDATED, CATEGORY_DELETED
    }

    public static ProductEvent created(Long productId, String productName, BigDecimal price, Long categoryId) {
        return ProductEvent.builder()
                .eventType(EventType.CREATED.name())
                .eventId(UUID.randomUUID())
                .productId(productId)
                .productName(productName)
                .price(price)
                .categoryId(categoryId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ProductEvent updated(Long productId, String productName, BigDecimal price, Long categoryId) {
        return ProductEvent.builder()
                .eventType(EventType.UPDATED.name())
                .eventId(UUID.randomUUID())
                .productId(productId)
                .productName(productName)
                .price(price)
                .categoryId(categoryId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ProductEvent deleted(Long productId) {
        return ProductEvent.builder()
                .eventType(EventType.DELETED.name())
                .eventId(UUID.randomUUID())
                .productId(productId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ProductEvent variantCreated(Long productId, Long variantId, String skuCode, BigDecimal price, Long categoryId) {
        return ProductEvent.builder()
                .eventType(EventType.VARIANT_CREATED.name())
                .eventId(UUID.randomUUID())
                .productId(productId)
                .variantId(variantId)
                .skuCode(skuCode)
                .price(price)
                .categoryId(categoryId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ProductEvent variantUpdated(Long productId, Long variantId, String skuCode, BigDecimal price, Long categoryId) {
        return ProductEvent.builder()
                .eventType(EventType.VARIANT_UPDATED.name())
                .eventId(UUID.randomUUID())
                .productId(productId)
                .variantId(variantId)
                .skuCode(skuCode)
                .price(price)
                .categoryId(categoryId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ProductEvent variantDeleted(Long productId, Long variantId, String skuCode) {
        return ProductEvent.builder()
                .eventType(EventType.VARIANT_DELETED.name())
                .eventId(UUID.randomUUID())
                .productId(productId)
                .variantId(variantId)
                .skuCode(skuCode)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ProductEvent categoryCreated(Long categoryId, String categoryName, Long parentId) {
        return ProductEvent.builder()
                .eventType(EventType.CATEGORY_CREATED.name())
                .eventId(UUID.randomUUID())
                .productId(categoryId)
                .productName(categoryName)
                .categoryId(parentId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ProductEvent categoryUpdated(Long categoryId, String categoryName, Long parentId) {
        return ProductEvent.builder()
                .eventType(EventType.CATEGORY_UPDATED.name())
                .eventId(UUID.randomUUID())
                .productId(categoryId)
                .productName(categoryName)
                .categoryId(parentId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ProductEvent categoryDeleted(Long categoryId) {
        return ProductEvent.builder()
                .eventType(EventType.CATEGORY_DELETED.name())
                .eventId(UUID.randomUUID())
                .productId(categoryId)
                .timestamp(LocalDateTime.now())
                .build();
    }
}