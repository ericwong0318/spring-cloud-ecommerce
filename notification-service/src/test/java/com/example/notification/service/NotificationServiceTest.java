package com.example.notification.service;

import com.example.common.dto.NotificationDto;
import com.example.notification.mapper.NotificationMapper;
import com.example.notification.model.Notification;
import com.example.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationService notificationService;

    private NotificationDto notificationDto;
    private Notification notification;

    @BeforeEach
    void setUp() {
        notificationDto = NotificationDto.builder()
                .id(1L)
                .recipient("customer@example.com")
                .subject("Test Subject")
                .content("Test Content")
                .type(NotificationDto.NotificationType.ORDER_CONFIRMATION)
                .channel(NotificationDto.NotificationChannel.EMAIL)
                .status(NotificationDto.NotificationStatus.PENDING)
                .referenceId("100")
                .referenceType("ORDER")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        notification = new Notification();
        notification.setId(1L);
        notification.setRecipient("customer@example.com");
        notification.setSubject("Test Subject");
        notification.setContent("Test Content");
        notification.setType(Notification.NotificationType.ORDER_CONFIRMATION);
        notification.setChannel(Notification.NotificationChannel.EMAIL);
        notification.setStatus(Notification.NotificationStatus.PENDING);
        notification.setReferenceId("100");
        notification.setReferenceType("ORDER");
        notification.setCreatedAt(LocalDateTime.now());
        notification.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void createNotification_shouldCreateAndReturnNotification() {
        when(notificationMapper.toEntity(notificationDto)).thenReturn(notification);
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(notificationMapper.toDto(notification)).thenReturn(notificationDto);

        NotificationDto result = notificationService.createNotification(notificationDto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getRecipient()).isEqualTo("customer@example.com");
        assertThat(result.getStatus()).isEqualTo(NotificationDto.NotificationStatus.PENDING);
        verify(notificationMapper).toEntity(notificationDto);
        verify(notificationRepository).save(any(Notification.class));
        verify(notificationMapper).toDto(notification);
    }

    @Test
    void sendNotification_shouldSendAndUpdateStatus_whenEmailSucceeds() {
        notification.setStatus(Notification.NotificationStatus.PENDING);
        doNothing().when(emailService).sendEmail(anyString(), anyString(), anyString());
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        notificationService.sendNotification(notification);

        assertThat(notification.getStatus()).isEqualTo(Notification.NotificationStatus.SENT);
        assertThat(notification.getSentAt()).isNotNull();
        verify(emailService).sendEmail("customer@example.com", "Test Subject", "Test Content");
        verify(notificationRepository).save(notification);
    }

    @Test
    void sendNotification_shouldUpdateStatusToFailed_whenEmailFails() {
        notification.setStatus(Notification.NotificationStatus.PENDING);
        doThrow(new RuntimeException("SMTP connection timeout")).when(emailService)
                .sendEmail(anyString(), anyString(), anyString());
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        notificationService.sendNotification(notification);

        assertThat(notification.getStatus()).isEqualTo(Notification.NotificationStatus.FAILED);
        assertThat(notification.getErrorMessage()).isEqualTo("SMTP connection timeout");
        verify(emailService).sendEmail(anyString(), anyString(), anyString());
        verify(notificationRepository, times(1)).save(notification);
    }

    @Test
    void retryFailedNotifications_shouldRetryFailedNotifications() {
        Notification failedNotification = new Notification();
        failedNotification.setId(2L);
        failedNotification.setRecipient("customer@example.com");
        failedNotification.setSubject("Retry Subject");
        failedNotification.setContent("Retry Content");
        failedNotification.setType(Notification.NotificationType.ORDER_CONFIRMATION);
        failedNotification.setChannel(Notification.NotificationChannel.EMAIL);
        failedNotification.setStatus(Notification.NotificationStatus.FAILED);
        failedNotification.setErrorMessage("Previous error");
        failedNotification.setCreatedAt(LocalDateTime.now());
        failedNotification.setUpdatedAt(LocalDateTime.now());

        when(notificationRepository.findByStatus(Notification.NotificationStatus.FAILED))
                .thenReturn(List.of(failedNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(failedNotification);
        doNothing().when(emailService).sendEmail(anyString(), anyString(), anyString());

        notificationService.retryFailedNotifications();

        // After retry, status becomes SENT (sendNotification succeeds)
        assertThat(failedNotification.getStatus()).isEqualTo(Notification.NotificationStatus.SENT);
        verify(notificationRepository).findByStatus(Notification.NotificationStatus.FAILED);
        // save is called twice: once for RETRYING, once for SENT
        verify(notificationRepository, times(2)).save(failedNotification);
        verify(emailService).sendEmail("customer@example.com", "Retry Subject", "Retry Content");
    }

    @Test
    void retryFailedNotifications_shouldDoNothing_whenNoFailedNotifications() {
        when(notificationRepository.findByStatus(Notification.NotificationStatus.FAILED))
                .thenReturn(Collections.emptyList());

        notificationService.retryFailedNotifications();

        verify(notificationRepository).findByStatus(Notification.NotificationStatus.FAILED);
        verify(notificationRepository, never()).save(any());
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void processPendingNotifications_shouldProcessPendingNotifications() {
        Notification pendingNotification = new Notification();
        pendingNotification.setId(3L);
        pendingNotification.setRecipient("customer@example.com");
        pendingNotification.setSubject("Pending Subject");
        pendingNotification.setContent("Pending Content");
        pendingNotification.setType(Notification.NotificationType.ORDER_CONFIRMATION);
        pendingNotification.setChannel(Notification.NotificationChannel.EMAIL);
        pendingNotification.setStatus(Notification.NotificationStatus.PENDING);
        pendingNotification.setCreatedAt(LocalDateTime.now());
        pendingNotification.setUpdatedAt(LocalDateTime.now());

        when(notificationRepository.findByStatus(Notification.NotificationStatus.PENDING))
                .thenReturn(List.of(pendingNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(pendingNotification);

        notificationService.processPendingNotifications();

        assertThat(pendingNotification.getStatus()).isEqualTo(Notification.NotificationStatus.SENT);
        assertThat(pendingNotification.getSentAt()).isNotNull();
        verify(notificationRepository).findByStatus(Notification.NotificationStatus.PENDING);
        verify(notificationRepository).save(pendingNotification);
        verify(emailService).sendEmail("customer@example.com", "Pending Subject", "Pending Content");
    }

    @Test
    void processPendingNotifications_shouldDoNothing_whenNoPendingNotifications() {
        when(notificationRepository.findByStatus(Notification.NotificationStatus.PENDING))
                .thenReturn(Collections.emptyList());

        notificationService.processPendingNotifications();

        verify(notificationRepository).findByStatus(Notification.NotificationStatus.PENDING);
        verify(notificationRepository, never()).save(any());
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
    }
}