package com.example.notification.listener;

import com.example.common.event.BaseEvent;
import com.example.common.event.IdempotentEventProcessor;
import com.example.common.event.OrderEvent;
import com.example.common.event.PaymentEvent;
import com.example.notification.model.Notification;
import com.example.notification.repository.NotificationRepository;
import com.example.notification.service.EmailService;
import com.example.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final IdempotentEventProcessor idempotentEventProcessor;

    public NotificationEventListener(NotificationRepository notificationRepository,
                                     NotificationService notificationService,
                                     EmailService emailService,
                                     IdempotentEventProcessor idempotentEventProcessor) {
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.idempotentEventProcessor = idempotentEventProcessor;
    }

    @RabbitListener(queues = "${rabbitmq.queue.notification-events}")
    @Transactional
    public void handleOrderEvent(OrderEvent event) {
        idempotentEventProcessor.process(event, this::handleOrderEventInternal);
    }

    private void handleOrderEventInternal(OrderEvent event) {
        log.info("Received order event: {}", event);

        switch (OrderEvent.EventType.valueOf(event.getEventType())) {
            case CREATED -> handleOrderCreated(event);
            case CANCELLED -> handleOrderCancelled(event);
            default -> log.debug("Unhandled order event type: {}", event.getEventType());
        }
    }

    @RabbitListener(queues = "${rabbitmq.queue.notification-events}")
    @Transactional
    public void handlePaymentEvent(PaymentEvent event) {
        idempotentEventProcessor.process(event, this::handlePaymentEventInternal);
    }

    private void handlePaymentEventInternal(PaymentEvent event) {
        log.info("Received payment event: {}", event);

        switch (PaymentEvent.EventType.valueOf(event.getEventType())) {
            case AUTHORIZED -> handlePaymentAuthorized(event);
            case CAPTURED -> handlePaymentSuccess(event);
            case FAILED -> handlePaymentFailed(event);
            case REFUNDED -> {
                if (event.getStatus() == PaymentEvent.PaymentStatus.PARTIALLY_REFUNDED) {
                    handlePaymentPartiallyRefunded(event);
                } else {
                    handlePaymentRefunded(event);
                }
            }
            default -> log.debug("Unhandled payment event type: {}", event.getEventType());
        }
    }

    private void handlePaymentAuthorized(PaymentEvent event) {
        notificationService.createNotificationFromTemplate(
                "PAYMENT_AUTHORIZED", "EMAIL",
                event.getCustomerEmail(),
                event.getOrderId().toString(), "PAYMENT");
        log.info("Created payment authorized notification for order: {}", event.getOrderId());
    }

    private void handlePaymentRefunded(PaymentEvent event) {
        notificationService.createNotificationFromTemplate(
                "PAYMENT_REFUNDED", "EMAIL",
                event.getCustomerEmail(),
                event.getOrderId().toString(), "PAYMENT");
        log.info("Created payment refunded notification for order: {}", event.getOrderId());
    }

    private void handlePaymentPartiallyRefunded(PaymentEvent event) {
        notificationService.createNotificationFromTemplate(
                "PAYMENT_PARTIALLY_REFUNDED", "EMAIL",
                event.getCustomerEmail(),
                event.getOrderId().toString(), "PAYMENT");
        log.info("Created payment partially refunded notification for order: {}", event.getOrderId());
    }

    private void handleOrderCreated(OrderEvent event) {
        notificationService.createNotificationFromTemplate(
                "ORDER_CONFIRMATION", "EMAIL",
                event.getCustomerEmail(),
                event.getOrderId().toString(), "ORDER");
        log.info("Created order confirmation notification for order: {}", event.getOrderId());
    }

    private void handleOrderCancelled(OrderEvent event) {
        notificationService.createNotificationFromTemplate(
                "ORDER_CONFIRMATION", "EMAIL",
                event.getCustomerEmail(),
                event.getOrderId().toString(), "ORDER");
        log.info("Created order cancellation notification for order: {}", event.getOrderId());
    }

    private void handlePaymentSuccess(PaymentEvent event) {
        notificationService.createNotificationFromTemplate(
                "PAYMENT_SUCCESS", "EMAIL",
                event.getCustomerEmail(),
                event.getOrderId().toString(), "PAYMENT");
        log.info("Created payment success notification for order: {}", event.getOrderId());
    }

    private void handlePaymentFailed(PaymentEvent event) {
        notificationService.createNotificationFromTemplate(
                "PAYMENT_FAILED", "EMAIL",
                event.getCustomerEmail(),
                event.getOrderId().toString(), "PAYMENT");
        log.info("Created payment failed notification for order: {}", event.getOrderId());
    }
}