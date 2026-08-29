package com.example.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Notification data transfer object")
public class NotificationDto {

    @Schema(description = "Unique identifier", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank(message = "Recipient is required")
    @Size(max = 255)
    @Schema(description = "Recipient email/phone", example = "customer@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String recipient;

    @NotBlank(message = "Subject is required")
    @Size(max = 500)
    @Schema(description = "Notification subject", example = "Order Confirmation", requiredMode = Schema.RequiredMode.REQUIRED)
    private String subject;

    @NotBlank(message = "Content is required")
    @Schema(description = "Notification content", example = "Your order has been confirmed", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @NotNull(message = "Type is required")
    @Schema(description = "Notification type", example = "EMAIL", requiredMode = Schema.RequiredMode.REQUIRED)
    private NotificationType type;

    @Schema(description = "Notification status", example = "PENDING", accessMode = Schema.AccessMode.READ_ONLY)
    private NotificationStatus status;

    @Schema(description = "Reference ID (order ID, payment ID, etc.)", example = "123")
    private String referenceId;

    @Schema(description = "Reference type (ORDER, PAYMENT, etc.)", example = "ORDER")
    private String referenceType;

    @Schema(description = "Error message if failed", example = "SMTP connection timeout", accessMode = Schema.AccessMode.READ_ONLY)
    private String errorMessage;

    @Schema(description = "Sent timestamp", example = "2024-01-15T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime sentAt;

    @Schema(description = "Creation timestamp", example = "2024-01-15T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp", example = "2024-01-15T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;

    public enum NotificationType {
        EMAIL, SMS, PUSH, IN_APP
    }

    public enum NotificationStatus {
        PENDING, SENT, FAILED, RETRYING
    }
}