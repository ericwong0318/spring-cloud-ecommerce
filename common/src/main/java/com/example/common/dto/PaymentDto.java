package com.example.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Payment data transfer object")
public class PaymentDto {

    @Schema(description = "Unique identifier", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotNull(message = "Order ID is required")
    @Schema(description = "Order ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long orderId;

    @NotNull(message = "Amount is required")
    @Schema(description = "Payment amount", example = "1999.98", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3)
    @Schema(description = "Currency code (ISO 4217)", example = "USD", requiredMode = Schema.RequiredMode.REQUIRED)
    private String currency;

    @Schema(description = "Payment status", example = "AUTHORIZED", accessMode = Schema.AccessMode.READ_ONLY)
    private PaymentStatus status;

    @Schema(description = "Gateway transaction ID", example = "txn_123456", accessMode = Schema.AccessMode.READ_ONLY)
    private String gatewayTransactionId;

    @Schema(description = "Idempotency key", example = "auth-1-abc123", accessMode = Schema.AccessMode.READ_ONLY)
    private String idempotencyKey;

    @Schema(description = "Authorized timestamp", example = "2024-01-15T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime authorizedAt;

    @Schema(description = "Captured timestamp", example = "2024-01-15T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime capturedAt;

    @Schema(description = "Refunded timestamp", example = "2024-01-20T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime refundedAt;

    @Schema(description = "Creation timestamp", example = "2024-01-15T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp", example = "2024-01-15T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;

    public PaymentDto() {
    }

    public PaymentDto(Long id, Long orderId, BigDecimal amount, String currency, PaymentStatus status,
                      String gatewayTransactionId, String idempotencyKey,
                      LocalDateTime authorizedAt, LocalDateTime capturedAt, LocalDateTime refundedAt,
                      LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.orderId = orderId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.gatewayTransactionId = gatewayTransactionId;
        this.idempotencyKey = idempotencyKey;
        this.authorizedAt = authorizedAt;
        this.capturedAt = capturedAt;
        this.refundedAt = refundedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
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

    public String getGatewayTransactionId() {
        return gatewayTransactionId;
    }

    public void setGatewayTransactionId(String gatewayTransactionId) {
        this.gatewayTransactionId = gatewayTransactionId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public LocalDateTime getAuthorizedAt() {
        return authorizedAt;
    }

    public void setAuthorizedAt(LocalDateTime authorizedAt) {
        this.authorizedAt = authorizedAt;
    }

    public LocalDateTime getCapturedAt() {
        return capturedAt;
    }

    public void setCapturedAt(LocalDateTime capturedAt) {
        this.capturedAt = capturedAt;
    }

    public LocalDateTime getRefundedAt() {
        return refundedAt;
    }

    public void setRefundedAt(LocalDateTime refundedAt) {
        this.refundedAt = refundedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public enum PaymentStatus {
        PENDING, AUTHORIZED, CAPTURED, REFUNDED, FAILED, PARTIALLY_REFUNDED
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long orderId;
        private BigDecimal amount;
        private String currency;
        private PaymentStatus status;
        private String gatewayTransactionId;
        private String idempotencyKey;
        private LocalDateTime authorizedAt;
        private LocalDateTime capturedAt;
        private LocalDateTime refundedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder orderId(Long orderId) {
            this.orderId = orderId;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder status(PaymentStatus status) {
            this.status = status;
            return this;
        }

        public Builder gatewayTransactionId(String gatewayTransactionId) {
            this.gatewayTransactionId = gatewayTransactionId;
            return this;
        }

        public Builder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        public Builder authorizedAt(LocalDateTime authorizedAt) {
            this.authorizedAt = authorizedAt;
            return this;
        }

        public Builder capturedAt(LocalDateTime capturedAt) {
            this.capturedAt = capturedAt;
            return this;
        }

        public Builder refundedAt(LocalDateTime refundedAt) {
            this.refundedAt = refundedAt;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public PaymentDto build() {
            return new PaymentDto(id, orderId, amount, currency, status, gatewayTransactionId,
                    idempotencyKey, authorizedAt, capturedAt, refundedAt, createdAt, updatedAt);
        }
    }
}