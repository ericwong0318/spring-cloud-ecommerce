package com.example.payment.listener;

import com.example.common.event.InventoryEvent;
import com.example.common.event.ReactiveIdempotentEventProcessor;
import com.example.common.listener.BaseReactiveSagaListener;
import com.example.payment.saga.PaymentSagaOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class InventoryEventListener extends BaseReactiveSagaListener<InventoryEvent> {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventListener.class);

    private final PaymentSagaOrchestrator paymentSagaOrchestrator;

    public InventoryEventListener(ReactiveIdempotentEventProcessor idempotentEventProcessor,
                                   PaymentSagaOrchestrator paymentSagaOrchestrator) {
        super(idempotentEventProcessor);
        this.paymentSagaOrchestrator = paymentSagaOrchestrator;
    }

    @RabbitListener(queues = "${rabbitmq.queue.inventory-reserved}")
    public void handleInventoryReserved(InventoryEvent event) {
        processEvent(event);
    }

    @Override
    protected Mono<Void> handleEventInternal(InventoryEvent event) {
        return paymentSagaOrchestrator.handleInventoryReserved(event);
    }
}