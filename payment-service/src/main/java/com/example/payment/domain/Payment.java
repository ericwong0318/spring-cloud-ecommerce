package com.example.payment.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Payment(
        Long id,
        Long orderId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String gatewayTransactionId,
        String idempotencyKey,
        LocalDateTime authorizedAt,
        LocalDateTime capturedAt,
        LocalDateTime refundedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public enum PaymentStatus {
        PENDING, AUTHORIZED, CAPTURED, REFUNDED, FAILED, PARTIALLY_REFUNDED
    }

    public static Payment authorize(Long orderId, BigDecimal amount, String currency, String idempotencyKey) {
        LocalDateTime now = LocalDateTime.now();
        return new Payment(null, orderId, amount, currency, PaymentStatus.AUTHORIZED,
                null, idempotencyKey, now, null, null, now, now);
    }

    public Payment capture(String gatewayTransactionId) {
        LocalDateTime now = LocalDateTime.now();
        return new Payment(id, orderId, amount, currency, PaymentStatus.CAPTURED,
                gatewayTransactionId, idempotencyKey, authorizedAt, now, refundedAt, createdAt, now);
    }

    public Payment refund(BigDecimal amount) {
        LocalDateTime now = LocalDateTime.now();
        PaymentStatus newStatus = amount.compareTo(this.amount) == 0
                ? PaymentStatus.REFUNDED
                : PaymentStatus.PARTIALLY_REFUNDED;
        return new Payment(id, orderId, this.amount, currency, newStatus,
                gatewayTransactionId, idempotencyKey, authorizedAt, capturedAt, now, createdAt, now);
    }

    public Payment withId(Long id) {
        return new Payment(id, orderId, amount, currency, status, gatewayTransactionId,
                idempotencyKey, authorizedAt, capturedAt, refundedAt, createdAt, updatedAt);
    }

    public boolean canCapture() {
        return status == PaymentStatus.AUTHORIZED;
    }

    public boolean canRefund() {
        return status == PaymentStatus.CAPTURED || status == PaymentStatus.PARTIALLY_REFUNDED;
    }

    public boolean isAmountValid(BigDecimal refundAmount) {
        return refundAmount.compareTo(amount) <= 0;
    }
}