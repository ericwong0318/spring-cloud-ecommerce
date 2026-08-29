package com.example.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryEvent {

    private String eventType;
    private Long productId;
    private String productName;
    private Integer quantity;
    private Integer reservedQuantity;
    private Integer availableQuantity;
    private BigDecimal costPrice;
    private LocalDateTime timestamp;

    public enum EventType {
        CREATED, UPDATED, DELETED,
        RESERVED, RELEASED, CONFIRMED,
        LOW_STOCK, STOCK_ADDED
    }

    public static InventoryEvent created(Long productId, String productName) {
        return InventoryEvent.builder()
                .eventType(EventType.CREATED.name())
                .productId(productId)
                .productName(productName)
                .quantity(0)
                .reservedQuantity(0)
                .availableQuantity(0)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static InventoryEvent reserved(Long productId, Integer reservedQuantity, Integer availableQuantity) {
        return InventoryEvent.builder()
                .eventType(EventType.RESERVED.name())
                .productId(productId)
                .reservedQuantity(reservedQuantity)
                .availableQuantity(availableQuantity)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static InventoryEvent released(Long productId, Integer reservedQuantity, Integer availableQuantity) {
        return InventoryEvent.builder()
                .eventType(EventType.RELEASED.name())
                .productId(productId)
                .reservedQuantity(reservedQuantity)
                .availableQuantity(availableQuantity)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static InventoryEvent confirmed(Long productId, Integer quantity, Integer availableQuantity) {
        return InventoryEvent.builder()
                .eventType(EventType.CONFIRMED.name())
                .productId(productId)
                .quantity(quantity)
                .availableQuantity(availableQuantity)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static InventoryEvent lowStock(Long productId, Integer availableQuantity, Integer reorderLevel) {
        return InventoryEvent.builder()
                .eventType(EventType.LOW_STOCK.name())
                .productId(productId)
                .availableQuantity(availableQuantity)
                .timestamp(LocalDateTime.now())
                .build();
    }
}