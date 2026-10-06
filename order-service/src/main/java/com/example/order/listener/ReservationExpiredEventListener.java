package com.example.order.listener;

import com.example.common.event.BaseEvent;
import com.example.common.event.ReservationExpiredEvent;
import com.example.common.event.ReactiveIdempotentEventProcessor;
import com.example.common.listener.BaseReactiveSagaListener;
import com.example.commonsaga.OrderSagaHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class ReservationExpiredEventListener extends BaseReactiveSagaListener<ReservationExpiredEvent> {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpiredEventListener.class);

    private final OrderSagaHandler sagaHandler;

    public ReservationExpiredEventListener(ReactiveIdempotentEventProcessor idempotentEventProcessor,
                                            OrderSagaHandler sagaHandler) {
        super(idempotentEventProcessor);
        this.sagaHandler = sagaHandler;
    }

    @RabbitListener(queues = "${rabbitmq.queue.reservation-expired}")
    public void handleReservationExpiredEvent(ReservationExpiredEvent event) {
        processEvent(event);
    }

    @Override
    protected Mono<Void> handleEventInternal(ReservationExpiredEvent event) {
        log.info("Received ReservationExpiredEvent: eventId={}, orderItemId={}, variantId={}, quantityReleased={}",
                event.getEventId(), event.getOrderItemId(), event.getVariantId(), event.getQuantityReleased());

        return sagaHandler.handleReservationExpired(event);
    }
}
