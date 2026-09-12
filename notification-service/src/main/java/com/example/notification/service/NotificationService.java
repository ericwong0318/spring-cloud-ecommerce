package com.example.notification.service;

import com.example.common.dto.NotificationDto;
import com.example.common.event.OutboxEventPublisher;
import com.example.notification.mapper.NotificationMapper;
import com.example.notification.model.Notification;
import com.example.notification.model.NotificationTemplate;
import com.example.notification.repository.NotificationRepository;
import com.example.notification.repository.NotificationTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository templateRepository;
    private final EmailService emailService;
    private final SmsService smsService;
    private final NotificationMapper notificationMapper;
    private final OutboxEventPublisher outboxEventPublisher;

    public NotificationService(NotificationRepository notificationRepository,
                               NotificationTemplateRepository templateRepository,
                               EmailService emailService,
                               SmsService smsService,
                               NotificationMapper notificationMapper,
                               OutboxEventPublisher outboxEventPublisher) {
        this.notificationRepository = notificationRepository;
        this.templateRepository = templateRepository;
        this.emailService = emailService;
        this.smsService = smsService;
        this.notificationMapper = notificationMapper;
        this.outboxEventPublisher = outboxEventPublisher;
    }

    @Transactional
    public void sendNotification(Notification notification) {
        log.info("Sending notification {} via {}", notification.getId(), notification.getChannel());

        try {
            boolean sent = sendViaChannel(notification);

            if (sent) {
                notification.setStatus(Notification.NotificationStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
                notificationRepository.save(notification);
                log.info("Sent notification {} to {} via {}", notification.getId(), notification.getRecipient(), notification.getChannel());
                publishNotificationEvent("Notification", notification.getId().toString(), "SENT", notification);
            } else {
                handleSendFailure(notification);
            }
        } catch (Exception e) {
            log.error("Failed to send notification {}", notification.getId(), e);
            handleSendFailure(notification, e.getMessage());
        }
    }

    private boolean sendViaChannel(Notification notification) {
        return switch (notification.getChannel()) {
            case EMAIL -> sendEmail(notification);
            case SMS -> sendSms(notification);
            case PUSH -> sendPush(notification);
            case IN_APP -> sendInApp(notification);
        };
    }

    private boolean sendEmail(Notification notification) {
        emailService.sendEmail(notification.getRecipient(), notification.getSubject(), notification.getContent());
        return true;
    }

    private boolean sendSms(Notification notification) {
        return smsService.sendSms(notification.getRecipient(), notification.getContent());
    }

    private boolean sendPush(Notification notification) {
        // TODO: Implement push notification
        log.warn("Push notification not implemented yet");
        return false;
    }

    private boolean sendInApp(Notification notification) {
        // TODO: Implement in-app notification
        log.warn("In-app notification not implemented yet");
        return false;
    }

    private void handleSendFailure(Notification notification) {
        handleSendFailure(notification, "Unknown error");
    }

    private void handleSendFailure(Notification notification, String errorMessage) {
        notification.setErrorMessage(errorMessage);
        notification.setRetryCount(notification.getRetryCount() + 1);

        if (notification.getRetryCount() >= notification.getMaxRetries()) {
            // Max retries reached - try fallback channel
            if (notification.getFallbackChannel() != null &&
                !notification.getFallbackChannel().equals(notification.getChannel().name())) {
                log.info("Max retries reached for notification {}, trying fallback channel: {}",
                        notification.getId(), notification.getFallbackChannel());
                notification.setChannel(Notification.NotificationChannel.valueOf(notification.getFallbackChannel()));
                notification.setRetryCount(0); // Reset retry count for fallback
                notification.setStatus(Notification.NotificationStatus.PENDING);
                notificationRepository.save(notification);
                sendNotification(notification);
            } else {
                // No fallback available - mark as failed
                notification.setStatus(Notification.NotificationStatus.FAILED);
                notificationRepository.save(notification);
                log.error("Notification {} failed after {} retries, no fallback available",
                        notification.getId(), notification.getMaxRetries());
                publishNotificationEvent("Notification", notification.getId().toString(), "FAILED", notification);
            }
        } else {
            // Retry later
            notification.setStatus(Notification.NotificationStatus.RETRYING);
            notificationRepository.save(notification);
            log.info("Notification {} will be retried (attempt {}/{})",
                    notification.getId(), notification.getRetryCount(), notification.getMaxRetries());
        }
    }

    private void publishNotificationEvent(String aggregateType, String aggregateId, String eventType, Notification notification) {
        outboxEventPublisher.saveEvent(aggregateType, aggregateId, eventType, notification);
    }

    @Transactional
    public NotificationDto createNotification(NotificationDto notificationDto) {
        Notification notification = notificationMapper.toEntity(notificationDto);
        notification.setStatus(Notification.NotificationStatus.PENDING);

        // Apply template if type is specified
        if (notificationDto.getType() != null) {
            applyTemplate(notification);
        }

        Notification saved = notificationRepository.save(notification);
        return notificationMapper.toDto(saved);
    }

    private void applyTemplate(Notification notification) {
        Optional<NotificationTemplate> templateOpt = templateRepository.findByTypeAndChannel(
                notification.getType().name(), notification.getChannel().name());

        if (templateOpt.isPresent()) {
            NotificationTemplate template = templateOpt.get();
            // Simple template variable replacement
            String subject = replaceVariables(template.getSubjectTemplate(), notification);
            String body = replaceVariables(template.getBodyTemplate(), notification);
            notification.setSubject(subject);
            notification.setContent(body);
        }

        applyDefaultSubjectAndContent(notification);
    }

    /**
     * Fallback when no template is configured: subject and content are
     * NOT NULL columns, so provide sensible defaults without overriding
     * values already supplied by the caller.
     */
    private void applyDefaultSubjectAndContent(Notification notification) {
        if (notification.getSubject() == null) {
            notification.setSubject(notification.getType().name() + " notification");
        }
        if (notification.getContent() == null) {
            notification.setContent(String.format(
                    "Notification %s for reference %s/%s to %s",
                    notification.getType().name(),
                    notification.getReferenceType(),
                    notification.getReferenceId(),
                    notification.getRecipient()));
        }
    }

    private String replaceVariables(String template, Notification notification) {
        if (template == null) return "";
        // Simple placeholder replacement: {{variable}} -> value
        // In a real implementation, use a proper template engine like Thymeleaf or Freemarker
        return template
                .replace("{{orderId}}", notification.getReferenceId())
                .replace("{{customerName}}", notification.getRecipient())
                .replace("{{amount}}", "0.00"); // Would come from reference data
    }

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    @Transactional
    public void processPendingNotifications() {
        log.debug("Processing pending notifications");
        List<Notification> pendingNotifications = notificationRepository.findByStatus(Notification.NotificationStatus.PENDING);
        for (Notification notification : pendingNotifications) {
            sendNotification(notification);
        }
    }

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    @Transactional
    public void retryFailedNotifications() {
        log.debug("Retrying failed/pending notifications");
        List<Notification> retryableNotifications = notificationRepository.findRetryableNotifications(Notification.NotificationStatus.RETRYING);
        for (Notification notification : retryableNotifications) {
            log.info("Retrying notification {} (attempt {}/{})",
                    notification.getId(), notification.getRetryCount(), notification.getMaxRetries());
            sendNotification(notification);
        }
    }

    @Transactional
    public NotificationDto createNotificationFromTemplate(String type, String channel, String recipient, String referenceId, String referenceType) {
        Notification notification = new Notification();
        notification.setType(Notification.NotificationType.valueOf(type));
        notification.setChannel(Notification.NotificationChannel.valueOf(channel));
        notification.setRecipient(recipient);
        notification.setReferenceId(referenceId);
        notification.setReferenceType(referenceType);
        notification.setStatus(Notification.NotificationStatus.PENDING);
        notification.setMaxRetries(3);
        notification.setFallbackChannel("SMS"); // Default fallback to SMS

        applyTemplate(notification);

        Notification saved = notificationRepository.save(notification);
        return notificationMapper.toDto(saved);
    }
}