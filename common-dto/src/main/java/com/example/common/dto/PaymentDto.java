package com.example.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Payment data transfer object")
public record PaymentDto(
    @Schema(description = "Unique identifier", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    Long id,

    @NotNull(message = "Order ID is required")
    @Schema(description = "Order ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    Long orderId,

    @NotNull(message = "Amount is required")
    @Schema(description = "Payment amount", example = "1999.98", requiredMode = Schema.RequiredMode.REQUIRED)
    BigDecimal amount,

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3)
    @Schema(description = "Currency code (ISO 4217)", example = "USD", requiredMode = Schema.RequiredMode.REQUIRED)
    String currency,

    @Schema(description = "Payment status", example = "AUTHORIZED", accessMode = Schema.AccessMode.READ_ONLY)
    PaymentStatus status,

    @Schema(description = "Gateway transaction ID", example = "txn_123456", accessMode = Schema.AccessMode.READ_ONLY)
    String gatewayTransactionId,

    @Schema(description = "Idempotency key", example = "auth-1-abc123", accessMode = Schema.AccessMode.READ_ONLY)
    String idempotencyKey,

    @Schema(description = "Authorized timestamp", example = "2024-01-15T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    LocalDateTime authorizedAt,

    @Schema(description = "Captured timestamp", example = "2024-01-15T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    LocalDateTime capturedAt,

    @Schema(description = "Refunded timestamp", example = "2024-01-20T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    LocalDateTime refundedAt,

    @Schema(description = "Creation timestamp", example = "2024-01-15T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    LocalDateTime createdAt,

    @Schema(description = "Last update timestamp", example = "2024-01-15T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    LocalDateTime updatedAt
) {
    public enum PaymentStatus {
        PENDING, AUTHORIZED, CAPTURED, REFUNDED, FAILED, PARTIALLY_REFUNDED
    }
}