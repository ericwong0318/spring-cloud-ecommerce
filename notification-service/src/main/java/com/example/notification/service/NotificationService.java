package com.example.notification.service;

import com.example.common.dto.NotificationDto;
import com.example.common.event.OutboxEventPublisher;
import com.example.notification.mapper.NotificationMapper;
import com.example.notification.model.Notification;
import com.example.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final NotificationMapper notificationMapper;
    private final OutboxEventPublisher outboxEventPublisher;

    @Transactional
    public void sendNotification(Notification notification) {
        try {
            notification.setStatus(Notification.NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            emailService.sendEmail(notification.getRecipient(), notification.getSubject(), notification.getContent());
            notificationRepository.save(notification);
            log.info("Sent notification {} to {}", notification.getId(), notification.getRecipient());
            publishNotificationEvent("Notification", notification.getId().toString(), "SENT", notification);
        } catch (Exception e) {
            log.error("Failed to send notification {}", notification.getId(), e);
            notification.setStatus(Notification.NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
            notificationRepository.save(notification);
            publishNotificationEvent("Notification", notification.getId().toString(), "FAILED", notification);
        }
    }

    private void publishNotificationEvent(String aggregateType, String aggregateId, String eventType, Notification notification) {
        outboxEventPublisher.saveEvent(aggregateType, aggregateId, eventType, notification);
    }

    @Transactional
    public NotificationDto createNotification(NotificationDto notificationDto) {
        Notification notification = notificationMapper.toEntity(notificationDto);
        notification.setStatus(Notification.NotificationStatus.PENDING);
        Notification saved = notificationRepository.save(notification);
        return notificationMapper.toDto(saved);
    }

    @Transactional
    public void retryFailedNotifications() {
        List<Notification> failedNotifications = notificationRepository.findByStatus(Notification.NotificationStatus.FAILED);
        for (Notification notification : failedNotifications) {
            log.info("Retrying failed notification: {}", notification.getId());
            notification.setStatus(Notification.NotificationStatus.RETRYING);
            notificationRepository.save(notification);
            sendNotification(notification);
        }
    }

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    @Transactional
    public void processPendingNotifications() {
        List<Notification> pendingNotifications = notificationRepository.findByStatus(Notification.NotificationStatus.PENDING);
        for (Notification notification : pendingNotifications) {
            sendNotification(notification);
        }
    }

    @Scheduled(fixedRate = 600000) // Every 10 minutes
    @Transactional
    public void retryStaleNotifications() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(30);
        List<Notification> staleNotifications = notificationRepository.findStalePendingNotifications(
                Notification.NotificationStatus.PENDING, threshold);
        for (Notification notification : staleNotifications) {
            log.info("Retrying stale notification: {}", notification.getId());
            sendNotification(notification);
        }
    }
}