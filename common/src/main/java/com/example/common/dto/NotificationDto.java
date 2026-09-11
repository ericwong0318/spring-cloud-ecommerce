package com.example.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Objects;

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
    @Schema(description = "Notification type", example = "ORDER_CONFIRMATION", requiredMode = Schema.RequiredMode.REQUIRED)
    private NotificationType type;

    @NotNull(message = "Channel is required")
    @Schema(description = "Notification channel", example = "EMAIL", requiredMode = Schema.RequiredMode.REQUIRED)
    private NotificationChannel channel;

    @Schema(description = "Notification status", example = "PENDING", accessMode = Schema.AccessMode.READ_ONLY)
    private NotificationStatus status;

    @Schema(description = "Reference ID (order ID, payment ID, etc.)", example = "123")
    private String referenceId;

    @Schema(description = "Reference type (ORDER, PAYMENT, etc.)", example = "ORDER")
    private String referenceType;

    @Schema(description = "Error message if failed", example = "SMTP connection timeout", accessMode = Schema.AccessMode.READ_ONLY)
    private String errorMessage;

    @Schema(description = "Retry count", example = "0", accessMode = Schema.AccessMode.READ_ONLY)
    private Integer retryCount;

    @Schema(description = "Max retries", example = "3", accessMode = Schema.AccessMode.READ_ONLY)
    private Integer maxRetries;

    @Schema(description = "Fallback channel", example = "SMS", accessMode = Schema.AccessMode.READ_ONLY)
    private String fallbackChannel;

    @Schema(description = "Sent timestamp", example = "2024-01-15T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime sentAt;

    @Schema(description = "Creation timestamp", example = "2024-01-15T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp", example = "2024-01-15T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;

    public NotificationDto() {
    }

    public NotificationDto(Long id, String recipient, String subject, String content,
                           NotificationType type, NotificationChannel channel, NotificationStatus status,
                           String referenceId, String referenceType, String errorMessage,
                           Integer retryCount, Integer maxRetries, String fallbackChannel,
                           LocalDateTime sentAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.recipient = recipient;
        this.subject = subject;
        this.content = content;
        this.type = type;
        this.channel = channel;
        this.status = status;
        this.referenceId = referenceId;
        this.referenceType = referenceType;
        this.errorMessage = errorMessage;
        this.retryCount = retryCount;
        this.maxRetries = maxRetries;
        this.fallbackChannel = fallbackChannel;
        this.sentAt = sentAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public void setChannel(NotificationChannel channel) {
        this.channel = channel;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }

    public String getFallbackChannel() {
        return fallbackChannel;
    }

    public void setFallbackChannel(String fallbackChannel) {
        this.fallbackChannel = fallbackChannel;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NotificationDto that = (NotificationDto) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "NotificationDto{" +
                "id=" + id +
                ", recipient='" + recipient + '\'' +
                ", subject='" + subject + '\'' +
                ", content='" + content + '\'' +
                ", type=" + type +
                ", channel=" + channel +
                ", status=" + status +
                ", referenceId='" + referenceId + '\'' +
                ", referenceType='" + referenceType + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", retryCount=" + retryCount +
                ", maxRetries=" + maxRetries +
                ", fallbackChannel='" + fallbackChannel + '\'' +
                ", sentAt=" + sentAt +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String recipient;
        private String subject;
        private String content;
        private NotificationType type;
        private NotificationChannel channel;
        private NotificationStatus status;
        private String referenceId;
        private String referenceType;
        private String errorMessage;
        private Integer retryCount;
        private Integer maxRetries;
        private String fallbackChannel;
        private LocalDateTime sentAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder recipient(String recipient) {
            this.recipient = recipient;
            return this;
        }

        public Builder subject(String subject) {
            this.subject = subject;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder type(NotificationType type) {
            this.type = type;
            return this;
        }

        public Builder channel(NotificationChannel channel) {
            this.channel = channel;
            return this;
        }

        public Builder status(NotificationStatus status) {
            this.status = status;
            return this;
        }

        public Builder referenceId(String referenceId) {
            this.referenceId = referenceId;
            return this;
        }

        public Builder referenceType(String referenceType) {
            this.referenceType = referenceType;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder retryCount(Integer retryCount) {
            this.retryCount = retryCount;
            return this;
        }

        public Builder maxRetries(Integer maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder fallbackChannel(String fallbackChannel) {
            this.fallbackChannel = fallbackChannel;
            return this;
        }

        public Builder sentAt(LocalDateTime sentAt) {
            this.sentAt = sentAt;
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

        public NotificationDto build() {
            return new NotificationDto(id, recipient, subject, content, type, channel, status,
                    referenceId, referenceType, errorMessage, retryCount, maxRetries,
                    fallbackChannel, sentAt, createdAt, updatedAt);
        }
    }

    public enum NotificationType {
        ORDER_CONFIRMATION, PAYMENT_SUCCESS, PAYMENT_FAILED, SHIPMENT_NOTIFICATION, LOW_STOCK_ALERT
    }

    public enum NotificationChannel {
        EMAIL, SMS, PUSH, IN_APP
    }

    public enum NotificationStatus {
        PENDING, SENT, FAILED, RETRYING
    }
}