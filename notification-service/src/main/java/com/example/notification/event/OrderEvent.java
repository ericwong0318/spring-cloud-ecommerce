package com.example.notification.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderEvent {
    private String eventType;
    private Long orderId;
    private String customerId;
    private String customerEmail;
    private Double totalAmount;
    private OrderStatus status;
    private List<OrderItem> items;
    private LocalDateTime timestamp;

    @Data
    public static class OrderItem {
        private Long productId;
        private String productName;
        private Integer quantity;
        private Double price;
    }

    public enum EventType {
        CREATED, UPDATED, CANCELLED
    }

    public enum OrderStatus {
        PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
    }
}