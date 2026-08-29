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
public class PaymentEvent {

    private String eventType;
    private Long paymentId;
    private Long orderId;
    private String customerId;
    private String customerEmail;
    private BigDecimal amount;
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

    public static PaymentEvent success(Long paymentId, Long orderId, String customerId, String customerEmail,
                                       BigDecimal amount, String currency, String transactionId) {
        return PaymentEvent.builder()
                .eventType(EventType.SUCCESS.name())
                .paymentId(paymentId)
                .orderId(orderId)
                .customerId(customerId)
                .customerEmail(customerEmail)
                .amount(amount)
                .currency(currency)
                .status(PaymentStatus.SUCCESS)
                .transactionId(transactionId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static PaymentEvent failed(Long paymentId, Long orderId, String customerId, String customerEmail,
                                      BigDecimal amount, String currency) {
        return PaymentEvent.builder()
                .eventType(EventType.FAILED.name())
                .paymentId(paymentId)
                .orderId(orderId)
                .customerId(customerId)
                .customerEmail(customerEmail)
                .amount(amount)
                .currency(currency)
                .status(PaymentStatus.FAILED)
                .timestamp(LocalDateTime.now())
                .build();
    }
}