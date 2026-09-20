package com.example.order.listener;

import com.example.common.event.PaymentEvent;
import com.example.common.event.ReactiveIdempotentEventProcessor;
import com.example.common.listener.BaseReactiveSagaListener;
import com.example.order.service.OrderSagaOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class PaymentEventListener extends BaseReactiveSagaListener<PaymentEvent> {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);

    private final OrderSagaOrchestrator sagaOrchestrator;

    public PaymentEventListener(ReactiveIdempotentEventProcessor idempotentEventProcessor,
                                 OrderSagaOrchestrator sagaOrchestrator) {
        super(idempotentEventProcessor);
        this.sagaOrchestrator = sagaOrchestrator;
    }

    @RabbitListener(queues = "${rabbitmq.queue.payment-events}")
    public void handlePaymentEvent(PaymentEvent event) {
        processEvent(event);
    }

    @Override
    protected Mono<Void> handleEventInternal(PaymentEvent event) {
        log.info("Received payment event: eventType={}, eventId={}, orderId={}, status={}",
                event.getEventType(), event.getEventId(), event.getOrderId(), event.getStatus());

        if (event.getOrderId() == null) {
            log.warn("Payment event missing orderId, skipping: eventId={}", event.getEventId());
            return Mono.empty();
        }

        return switch (PaymentEvent.EventType.valueOf(event.getEventType())) {
            case CAPTURED -> sagaOrchestrator.handlePaymentCaptured(event);
            case FAILED -> sagaOrchestrator.handlePaymentFailed(event);
            case REFUNDED -> {
                if (event.getStatus() == PaymentEvent.PaymentStatus.PARTIALLY_REFUNDED) {
                    yield sagaOrchestrator.handlePaymentPartiallyRefunded(event);
                } else {
                    yield sagaOrchestrator.handlePaymentRefunded(event);
                }
            }
            case AUTHORIZED -> {
                log.info("Payment authorized for order: {}", event.getOrderId());
                yield sagaOrchestrator.handlePaymentAuthorized(event);
            }
            default -> {
                log.debug("Unhandled payment event type: {}", event.getEventType());
                yield Mono.empty();
            }
        };
    }
}