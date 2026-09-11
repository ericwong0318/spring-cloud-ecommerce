package com.example.notification.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient", nullable = false)
    private String recipient;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    private NotificationChannel channel = NotificationChannel.EMAIL;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "reference_id")
    private String referenceId;

    @Column(name = "reference_type")
    private String referenceType;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "max_retries", nullable = false)
    private Integer maxRetries = 3;

    @Column(name = "fallback_channel", length = 20)
    private String fallbackChannel;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Notification() {
    }

    public Notification(Long id, String recipient, String subject, String content,
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

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Notification that = (Notification) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Notification{" +
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
        private NotificationChannel channel = NotificationChannel.EMAIL;
        private NotificationStatus status = NotificationStatus.PENDING;
        private String referenceId;
        private String referenceType;
        private String errorMessage;
        private Integer retryCount = 0;
        private Integer maxRetries = 3;
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

        public Notification build() {
            return new Notification(id, recipient, subject, content, type, channel, status,
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