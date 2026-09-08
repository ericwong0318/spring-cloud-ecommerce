package com.example.common.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class PaymentEvent implements BaseEvent {

    private String eventType;
    private UUID eventId;
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
        AUTHORIZED, CAPTURED, REFUNDED, FAILED
    }

    public enum PaymentStatus {
        AUTHORIZED, CAPTURED, REFUNDED, FAILED, PENDING, PARTIALLY_REFUNDED
    }

    public PaymentEvent() {
    }

    public PaymentEvent(String eventType, UUID eventId, Long paymentId, Long orderId,
                        String customerId, String customerEmail, BigDecimal amount,
                        String currency, PaymentStatus status, String transactionId,
                        LocalDateTime timestamp) {
        this.eventType = eventType;
        this.eventId = eventId;
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.customerId = customerId;
        this.customerEmail = customerEmail;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.transactionId = transactionId;
        this.timestamp = timestamp;
    }

    @Override
    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    @Override
    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public static PaymentEvent authorized(Long paymentId, Long orderId, String customerId, String customerEmail,
                                           BigDecimal amount, String currency, String transactionId) {
        PaymentEvent event = new PaymentEvent();
        event.setEventType(EventType.AUTHORIZED.name());
        event.setEventId(UUID.randomUUID());
        event.setPaymentId(paymentId);
        event.setOrderId(orderId);
        event.setCustomerId(customerId);
        event.setCustomerEmail(customerEmail);
        event.setAmount(amount);
        event.setCurrency(currency);
        event.setStatus(PaymentStatus.AUTHORIZED);
        event.setTransactionId(transactionId);
        event.setTimestamp(LocalDateTime.now());
        return event;
    }

    public static PaymentEvent captured(Long paymentId, Long orderId, String customerId, String customerEmail,
                                         BigDecimal amount, String currency, String transactionId) {
        PaymentEvent event = new PaymentEvent();
        event.setEventType(EventType.CAPTURED.name());
        event.setEventId(UUID.randomUUID());
        event.setPaymentId(paymentId);
        event.setOrderId(orderId);
        event.setCustomerId(customerId);
        event.setCustomerEmail(customerEmail);
        event.setAmount(amount);
        event.setCurrency(currency);
        event.setStatus(PaymentStatus.CAPTURED);
        event.setTransactionId(transactionId);
        event.setTimestamp(LocalDateTime.now());
        return event;
    }

    public static PaymentEvent failed(Long paymentId, Long orderId, String customerId, String customerEmail,
                                       BigDecimal amount, String currency) {
        PaymentEvent event = new PaymentEvent();
        event.setEventType(EventType.FAILED.name());
        event.setEventId(UUID.randomUUID());
        event.setPaymentId(paymentId);
        event.setOrderId(orderId);
        event.setCustomerId(customerId);
        event.setCustomerEmail(customerEmail);
        event.setAmount(amount);
        event.setCurrency(currency);
        event.setStatus(PaymentStatus.FAILED);
        event.setTimestamp(LocalDateTime.now());
        return event;
    }

    public static PaymentEvent refunded(Long paymentId, Long orderId, String customerId, String customerEmail,
                                         BigDecimal amount, String currency, String transactionId) {
        PaymentEvent event = new PaymentEvent();
        event.setEventType(EventType.REFUNDED.name());
        event.setEventId(UUID.randomUUID());
        event.setPaymentId(paymentId);
        event.setOrderId(orderId);
        event.setCustomerId(customerId);
        event.setCustomerEmail(customerEmail);
        event.setAmount(amount);
        event.setCurrency(currency);
        event.setStatus(PaymentStatus.REFUNDED);
        event.setTransactionId(transactionId);
        event.setTimestamp(LocalDateTime.now());
        return event;
    }

    public static PaymentEvent partiallyRefunded(Long paymentId, Long orderId, String customerId, String customerEmail,
                                                  BigDecimal amount, String currency, String transactionId) {
        PaymentEvent event = new PaymentEvent();
        event.setEventType(EventType.REFUNDED.name());
        event.setEventId(UUID.randomUUID());
        event.setPaymentId(paymentId);
        event.setOrderId(orderId);
        event.setCustomerId(customerId);
        event.setCustomerEmail(customerEmail);
        event.setAmount(amount);
        event.setCurrency(currency);
        event.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
        event.setTransactionId(transactionId);
        event.setTimestamp(LocalDateTime.now());
        return event;
    }
}