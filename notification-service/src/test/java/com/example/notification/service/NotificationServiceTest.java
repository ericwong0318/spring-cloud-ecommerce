package com.example.notification.service;

import com.example.common.dto.NotificationDto;
import com.example.notification.mapper.NotificationMapper;
import com.example.notification.model.Notification;
import com.example.notification.model.NotificationTemplate;
import com.example.notification.repository.NotificationRepository;
import com.example.notification.repository.NotificationTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationTemplateRepository templateRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private SmsService smsService;

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
        when(templateRepository.findByTypeAndChannel(anyString(), anyString())).thenReturn(Optional.empty());
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
    void processPendingNotifications_shouldDoNothing_whenNoPendingNotifications() {
        when(notificationRepository.findByStatus(Notification.NotificationStatus.PENDING))
                .thenReturn(Collections.emptyList());

        notificationService.processPendingNotifications();

        verify(notificationRepository).findByStatus(Notification.NotificationStatus.PENDING);
        verify(notificationRepository, never()).save(any());
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void createNotificationFromTemplate_shouldApplyDefaultSubjectAndContent_whenTemplateMissing() {
        when(templateRepository.findByTypeAndChannel(anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(notificationMapper.toDto(any(Notification.class))).thenReturn(notificationDto);

        notificationService.createNotificationFromTemplate(
                "ORDER_CONFIRMATION", "EMAIL", "customer@example.com", "100", "ORDER");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getSubject()).isEqualTo("ORDER_CONFIRMATION notification");
        assertThat(saved.getContent()).contains("ORDER").contains("100").contains("customer@example.com");
    }

    @Test
    void createNotification_shouldNotOverrideCallerProvidedSubjectAndContent_whenTemplateMissing() {
        when(templateRepository.findByTypeAndChannel(anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(notificationMapper.toEntity(notificationDto)).thenReturn(notification);
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(notificationMapper.toDto(any(Notification.class))).thenReturn(notificationDto);

        notificationService.createNotification(notificationDto);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getSubject()).isEqualTo("Test Subject");
        assertThat(saved.getContent()).isEqualTo("Test Content");
    }
}
