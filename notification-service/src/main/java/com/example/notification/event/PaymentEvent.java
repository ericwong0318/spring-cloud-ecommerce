package com.example.notification.event;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PaymentEvent {
    private String eventType;
    private Long paymentId;
    private Long orderId;
    private String customerId;
    private String customerEmail;
    private Double amount;
    private String currency;
    private PaymentStatus status;
    private String transactionId;
    private LocalDateTime timestamp;

    public enum EventType {
        SUCCESS, FAILED, REFUNDED
    }

    public enum PaymentStatus {
        SUCCESS, FAILED, REFUNDED, PENDING
    }
}