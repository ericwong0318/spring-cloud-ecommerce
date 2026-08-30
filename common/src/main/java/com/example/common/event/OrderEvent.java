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
public class OrderEvent implements BaseEvent {

    private String eventType;
    private UUID eventId;
    private Long orderId;
    private String customerId;
    private String customerEmail;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private List<OrderItem> items;
    private LocalDateTime timestamp;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem {
        private Long productId;
        private Long variantId;
        private String productName;
        private String skuCode;
        private Integer quantity;
        private Integer quantityShipped;
        private BigDecimal price;
        private OrderItemStatus status;
    }

    public enum EventType {
        CREATED, UPDATED, CANCELLED, SHIPPED, DELIVERED
    }

    public enum OrderStatus {
        PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
    }

    public enum OrderItemStatus {
        PENDING, RESERVED, SHIPPED, BACKORDERED, CANCELLED
    }

    public static OrderEvent created(Long orderId, String customerId, String customerEmail,
                                       BigDecimal totalAmount, List<OrderItem> items) {
        return OrderEvent.builder()
                .eventType(EventType.CREATED.name())
                .eventId(UUID.randomUUID())
                .orderId(orderId)
                .customerId(customerId)
                .customerEmail(customerEmail)
                .totalAmount(totalAmount)
                .status(OrderStatus.PENDING)
                .items(items)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static OrderEvent cancelled(Long orderId, String customerId, String customerEmail,
                                         List<OrderItem> items) {
        return OrderEvent.builder()
                .eventType(EventType.CANCELLED.name())
                .eventId(UUID.randomUUID())
                .orderId(orderId)
                .customerId(customerId)
                .customerEmail(customerEmail)
                .items(items)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static OrderEvent statusChanged(Long orderId, OrderStatus status) {
        return OrderEvent.builder()
                .eventType(EventType.UPDATED.name())
                .eventId(UUID.randomUUID())
                .orderId(orderId)
                .status(status)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static OrderEvent shipped(Long orderId, String customerId, String customerEmail,
                                       BigDecimal totalAmount, List<OrderItem> items) {
        return OrderEvent.builder()
                .eventType(EventType.SHIPPED.name())
                .eventId(UUID.randomUUID())
                .orderId(orderId)
                .customerId(customerId)
                .customerEmail(customerEmail)
                .totalAmount(totalAmount)
                .status(OrderStatus.SHIPPED)
                .items(items)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static OrderEvent delivered(Long orderId, String customerId, String customerEmail,
                                         BigDecimal totalAmount, List<OrderItem> items) {
        return OrderEvent.builder()
                .eventType(EventType.DELIVERED.name())
                .eventId(UUID.randomUUID())
                .orderId(orderId)
                .customerId(customerId)
                .customerEmail(customerEmail)
                .totalAmount(totalAmount)
                .status(OrderStatus.DELIVERED)
                .items(items)
                .timestamp(LocalDateTime.now())
                .build();
    }
}