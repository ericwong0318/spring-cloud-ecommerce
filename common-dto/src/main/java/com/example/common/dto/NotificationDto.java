package com.example.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Schema(description = "Notification data transfer object")
public record NotificationDto(
    @Schema(description = "Unique identifier", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    Long id,

    @NotBlank(message = "Recipient is required")
    @Size(max = 255)
    @Schema(description = "Recipient email/phone", example = "customer@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    String recipient,

    @NotBlank(message = "Subject is required")
    @Size(max = 500)
    @Schema(description = "Notification subject", example = "Order Confirmation", requiredMode = Schema.RequiredMode.REQUIRED)
    String subject,

    @NotBlank(message = "Content is required")
    @Schema(description = "Notification content", example = "Your order has been confirmed", requiredMode = Schema.RequiredMode.REQUIRED)
    String content,

    @NotNull(message = "Type is required")
    @Schema(description = "Notification type", example = "ORDER_CONFIRMATION", requiredMode = Schema.RequiredMode.REQUIRED)
    NotificationType type,

    @NotNull(message = "Channel is required")
    @Schema(description = "Notification channel", example = "EMAIL", requiredMode = Schema.RequiredMode.REQUIRED)
    NotificationChannel channel,

    @Schema(description = "Notification status", example = "PENDING", accessMode = Schema.AccessMode.READ_ONLY)
    NotificationStatus status,

    @Schema(description = "Reference ID (order ID, payment ID, etc.)", example = "123")
    String referenceId,

    @Schema(description = "Reference type (ORDER, PAYMENT, etc.)", example = "ORDER")
    String referenceType,

    @Schema(description = "Error message if failed", example = "SMTP connection timeout", accessMode = Schema.AccessMode.READ_ONLY)
    String errorMessage,

    @Schema(description = "Retry count", example = "0", accessMode = Schema.AccessMode.READ_ONLY)
    Integer retryCount,

    @Schema(description = "Max retries", example = "3", accessMode = Schema.AccessMode.READ_ONLY)
    Integer maxRetries,

    @Schema(description = "Fallback channel", example = "SMS", accessMode = Schema.AccessMode.READ_ONLY)
    String fallbackChannel,

    @Schema(description = "Sent timestamp", example = "2024-01-15T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    LocalDateTime sentAt,

    @Schema(description = "Creation timestamp", example = "2024-01-15T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    LocalDateTime createdAt,

    @Schema(description = "Last update timestamp", example = "2024-01-15T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    LocalDateTime updatedAt
) {
    public enum NotificationType {
        ORDER_CONFIRMATION, PAYMENT_SUCCESS, PAYMENT_FAILED, PAYMENT_AUTHORIZED,
        PAYMENT_REFUNDED, PAYMENT_PARTIALLY_REFUNDED, SHIPMENT_NOTIFICATION, LOW_STOCK_ALERT
    }

    public enum NotificationChannel {
        EMAIL, SMS, PUSH, IN_APP
    }

    public enum NotificationStatus {
        PENDING, SENT, FAILED, RETRYING
    }
}