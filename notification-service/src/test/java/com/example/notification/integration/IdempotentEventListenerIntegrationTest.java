package com.example.notification.integration;

import com.example.common.event.IdempotentEventProcessor;
import com.example.common.event.OrderEvent;
import com.example.common.event.PaymentEvent;
import com.example.notification.NotificationServiceApplication;
import com.example.notification.model.Notification;
import com.example.notification.repository.NotificationRepository;
import com.example.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(classes = NotificationServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class IdempotentEventListenerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("notification_db")
            .withUsername("test")
            .withPassword("test");

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.13-management")
            .withExposedPorts(5672);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.mail.host", () -> "localhost");
        registry.add("spring.mail.port", () -> "25");
    }

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private IdempotentEventProcessor idempotentEventProcessor;

    @Value("${rabbitmq.queue.notification-events}")
    private String notificationQueue;

    private final Long testOrderId = 1000L;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void shouldProcessOrderCreatedEventOnlyOnceWhenDuplicateEventId() {
        // Given: An order created event with a specific eventId
        UUID eventId = UUID.randomUUID();
        OrderEvent.OrderItem item = OrderEvent.OrderItem.builder()
                .orderItemId(1L)
                .productId(100L)
                .variantId(200L)
                .productName("Test Product")
                .skuCode("TEST-001")
                .quantity(5)
                .quantityShipped(0)
                .price(new BigDecimal("99.99"))
                .status(OrderEvent.OrderItemStatus.PENDING)
                .build();

        OrderEvent event = OrderEvent.created(testOrderId, "customer-1", "test@example.com",
                new BigDecimal("499.95"), List.of(item));
        event.setEventId(eventId);

        // When: Send the same event twice
        rabbitTemplate.convertAndSend(notificationQueue, event);
        rabbitTemplate.convertAndSend(notificationQueue, event);

        // Then: Wait for processing and verify notification was created only once
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findAll();
            assertThat(notifications).hasSize(1);
            Notification notification = notifications.get(0);
            assertThat(notification.getRecipient()).isEqualTo("test@example.com");
            assertThat(notification.getReferenceId()).isEqualTo(testOrderId.toString());
            assertThat(notification.getType()).isEqualTo(Notification.NotificationType.ORDER_CONFIRMATION);
        });
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void shouldProcessPaymentAuthorizedEventOnlyOnceWhenDuplicateEventId() {
        // Given: A payment authorized event with a specific eventId
        UUID eventId = UUID.randomUUID();
        PaymentEvent event = PaymentEvent.authorized(
                500L, testOrderId, "customer-1", "test@example.com",
                new BigDecimal("499.95"), "USD", "txn-123");
        event.setEventId(eventId);

        // When: Send the same event twice
        rabbitTemplate.convertAndSend(notificationQueue, event);
        rabbitTemplate.convertAndSend(notificationQueue, event);

        // Then: Wait for processing and verify notification was created only once
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findAll();
            assertThat(notifications).hasSize(1);
            Notification notification = notifications.get(0);
            assertThat(notification.getRecipient()).isEqualTo("test@example.com");
            assertThat(notification.getReferenceId()).isEqualTo(testOrderId.toString());
            assertThat(notification.getType()).isEqualTo(Notification.NotificationType.PAYMENT_SUCCESS);
        });
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void shouldProcessEventWithoutEventIdEveryTime() {
        // Given: An order event WITHOUT eventId (null)
        OrderEvent.OrderItem item = OrderEvent.OrderItem.builder()
                .orderItemId(2L)
                .productId(101L)
                .variantId(201L)
                .productName("Test Product 2")
                .skuCode("TEST-002")
                .quantity(3)
                .quantityShipped(0)
                .price(new BigDecimal("149.99"))
                .status(OrderEvent.OrderItemStatus.PENDING)
                .build();

        OrderEvent event = OrderEvent.created(testOrderId + 1, "customer-2", "test2@example.com",
                new BigDecimal("449.97"), List.of(item));
        event.setEventId(null); // No eventId - should process every time

        // When: Send the same event twice (without eventId)
        rabbitTemplate.convertAndSend(notificationQueue, event);
        rabbitTemplate.convertAndSend(notificationQueue, event);

        // Then: Both should be processed (no deduplication without eventId)
        // Since it's order creation, second one will create another notification
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findAll();
            // Two notifications created since no deduplication
            assertThat(notifications).hasSize(2);
        });
    }
}
