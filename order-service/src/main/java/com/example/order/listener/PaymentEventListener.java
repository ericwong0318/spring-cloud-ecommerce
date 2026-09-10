package com.example.order.listener;

import com.example.common.event.BaseEvent;
import com.example.common.event.PaymentEvent;
import com.example.order.event.ReactiveIdempotentEventProcessor;
import com.example.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
public class PaymentEventListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);

    private final ReactiveIdempotentEventProcessor idempotentEventProcessor;
    private final OrderService orderService;

    public PaymentEventListener(ReactiveIdempotentEventProcessor idempotentEventProcessor,
                                OrderService orderService) {
        this.idempotentEventProcessor = idempotentEventProcessor;
        this.orderService = orderService;
    }

    @RabbitListener(queues = "${rabbitmq.queue.payment-events}")
    public void handlePaymentEvent(PaymentEvent event) {
        idempotentEventProcessor.process(event, this::handlePaymentEventInternal)
                .subscribe(
                        unused -> log.debug("Successfully processed payment event: eventId={}", event.getEventId()),
                        error -> log.error("Failed to process payment event: eventId={}, error={}",
                                event.getEventId(), error.getMessage())
                );
    }

    private Mono<Void> handlePaymentEventInternal(PaymentEvent event) {
        log.info("Received payment event: eventType={}, eventId={}, orderId={}, status={}",
                event.getEventType(), event.getEventId(), event.getOrderId(), event.getStatus());

        if (event.getOrderId() == null) {
            log.warn("Payment event missing orderId, skipping: eventId={}", event.getEventId());
            return Mono.empty();
        }

        return switch (PaymentEvent.EventType.valueOf(event.getEventType())) {
            case CAPTURED -> orderService.handlePaymentCaptured(event.getOrderId(), event.getAmount());
            case FAILED -> orderService.handlePaymentFailed(event.getOrderId());
            case REFUNDED -> {
                if (event.getStatus() == PaymentEvent.PaymentStatus.PARTIALLY_REFUNDED) {
                    yield orderService.handlePaymentPartiallyRefunded(event.getOrderId());
                } else {
                    yield orderService.handlePaymentRefunded(event.getOrderId());
                }
            }
            case AUTHORIZED -> {
                log.info("Payment authorized for order: {}, no order state change needed", event.getOrderId());
                yield orderService.handlePaymentAuthorized(event.getOrderId());
            }
            default -> {
                log.debug("Unhandled payment event type: {}", event.getEventType());
                yield Mono.empty();
            }
        };
    }
}
