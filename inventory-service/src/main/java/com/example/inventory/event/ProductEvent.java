package com.example.inventory.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProductEvent {
    private String eventType;
    private Long productId;
    private String productName;
    private Double price;
    private Long categoryId;
    private LocalDateTime timestamp;

    public enum EventType {
        CREATED, UPDATED, DELETED
    }
}