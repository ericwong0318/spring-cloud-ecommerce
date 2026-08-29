package com.example.common.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductEvent {

    private String eventType;
    private Long productId;
    private String productName;
    private BigDecimal price;
    private Long categoryId;
    private LocalDateTime timestamp;

    public enum EventType {
        CREATED, UPDATED, DELETED
    }

    public static ProductEvent created(Long productId, String productName, BigDecimal price, Long categoryId) {
        return ProductEvent.builder()
                .eventType(EventType.CREATED.name())
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
                .productId(productId)
                .timestamp(LocalDateTime.now())
                .build();
    }
}