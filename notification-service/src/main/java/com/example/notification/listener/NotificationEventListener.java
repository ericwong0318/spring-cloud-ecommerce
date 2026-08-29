package com.example.notification.listener;

import com.example.notification.event.OrderEvent;
import com.example.notification.event.PaymentEvent;
import com.example.notification.model.Notification;
import com.example.notification.repository.NotificationRepository;
import com.example.notification.service.EmailService;
import com.example.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    @RabbitListener(queues = "${rabbitmq.queue.notification-events}")
    @Transactional
    public void handleOrderEvent(OrderEvent event) {
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
        log.info("Received payment event: {}", event);
        
        switch (PaymentEvent.EventType.valueOf(event.getEventType())) {
            case SUCCESS -> handlePaymentSuccess(event);
            case FAILED -> handlePaymentFailed(event);
            default -> log.debug("Unhandled payment event type: {}", event.getEventType());
        }
    }

    private void handleOrderCreated(OrderEvent event) {
        Notification notification = new Notification();
        notification.setRecipient(event.getCustomerEmail());
        notification.setSubject("Order Confirmation - Order #" + event.getOrderId());
        notification.setContent(buildOrderConfirmationEmail(event));
        notification.setType(Notification.NotificationType.EMAIL);
        notification.setReferenceId(event.getOrderId().toString());
        notification.setReferenceType("ORDER");
        notificationRepository.save(notification);
        
        notificationService.sendNotification(notification);
        log.info("Created order confirmation notification for order: {}", event.getOrderId());
    }

    private void handleOrderCancelled(OrderEvent event) {
        Notification notification = new Notification();
        notification.setRecipient(event.getCustomerEmail());
        notification.setSubject("Order Cancelled - Order #" + event.getOrderId());
        notification.setContent(buildOrderCancellationEmail(event));
        notification.setType(Notification.NotificationType.EMAIL);
        notification.setReferenceId(event.getOrderId().toString());
        notification.setReferenceType("ORDER");
        notificationRepository.save(notification);
        
        notificationService.sendNotification(notification);
        log.info("Created order cancellation notification for order: {}", event.getOrderId());
    }

    private void handlePaymentSuccess(PaymentEvent event) {
        Notification notification = new Notification();
        notification.setRecipient(event.getCustomerEmail());
        notification.setSubject("Payment Successful - Order #" + event.getOrderId());
        notification.setContent(buildPaymentSuccessEmail(event));
        notification.setType(Notification.NotificationType.EMAIL);
        notification.setReferenceId(event.getOrderId().toString());
        notification.setReferenceType("PAYMENT");
        notificationRepository.save(notification);
        
        notificationService.sendNotification(notification);
        log.info("Created payment success notification for order: {}", event.getOrderId());
    }

    private void handlePaymentFailed(PaymentEvent event) {
        Notification notification = new Notification();
        notification.setRecipient(event.getCustomerEmail());
        notification.setSubject("Payment Failed - Order #" + event.getOrderId());
        notification.setContent(buildPaymentFailedEmail(event));
        notification.setType(Notification.NotificationType.EMAIL);
        notification.setReferenceId(event.getOrderId().toString());
        notification.setReferenceType("PAYMENT");
        notificationRepository.save(notification);
        
        notificationService.sendNotification(notification);
        log.info("Created payment failed notification for order: {}", event.getOrderId());
    }

    private String buildOrderConfirmationEmail(OrderEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("Dear Customer,\n\n");
        sb.append("Thank you for your order! Your order #").append(event.getOrderId()).append(" has been confirmed.\n\n");
        sb.append("Order Details:\n");
        for (OrderEvent.OrderItem item : event.getItems()) {
            sb.append("- ").append(item.getProductName())
              .append(" x ").append(item.getQuantity())
              .append(" @ $").append(String.format("%.2f", item.getPrice())).append("\n");
        }
        sb.append("\nTotal: $").append(String.format("%.2f", event.getTotalAmount())).append("\n\n");
        sb.append("We'll notify you when your order ships.\n\n");
        sb.append("Thank you for shopping with us!\n");
        sb.append("The E-Commerce Team");
        return sb.toString();
    }

    private String buildOrderCancellationEmail(OrderEvent event) {
        return String.format("Dear Customer,\n\n" +
                "Your order #%d has been cancelled.\n\n" +
                "If you did not request this cancellation, please contact our support team.\n\n" +
                "Thank you for shopping with us!\n" +
                "The E-Commerce Team", event.getOrderId());
    }

    private String buildPaymentSuccessEmail(PaymentEvent event) {
        return String.format("Dear Customer,\n\n" +
                "Your payment of $%.2f %s for order #%d has been processed successfully.\n" +
                "Transaction ID: %s\n\n" +
                "Thank you for your purchase!\n" +
                "The E-Commerce Team", 
                event.getAmount(), event.getCurrency(), event.getOrderId(), event.getTransactionId());
    }

    private String buildPaymentFailedEmail(PaymentEvent event) {
        return String.format("Dear Customer,\n\n" +
                "We were unable to process your payment of $%.2f %s for order #%d.\n\n" +
                "Please update your payment method and try again, or contact our support team for assistance.\n\n" +
                "The E-Commerce Team", 
                event.getAmount(), event.getCurrency(), event.getOrderId());
    }
}