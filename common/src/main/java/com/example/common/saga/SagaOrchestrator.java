package com.example.common.saga;

import com.example.common.dto.OrderDto;
import com.example.common.event.BaseEvent;
import com.example.common.event.InventoryEvent;
import com.example.common.event.OrderEvent;
import com.example.common.event.PaymentEvent;
import com.example.common.event.ReservationExpiredEvent;
import reactor.core.publisher.Mono;

public interface SagaOrchestrator {

    Mono<OrderDto> createOrder(OrderDto orderDto);

    Mono<Void> handleOrderCreated(OrderEvent event);

    Mono<Void> handleInventoryReserved(InventoryEvent event);

    Mono<Void> handleInventoryReleased(InventoryEvent event);

    Mono<Void> handlePaymentAuthorized(PaymentEvent event);

    Mono<Void> handlePaymentCaptured(PaymentEvent event);

    Mono<Void> handlePaymentFailed(PaymentEvent event);

    Mono<Void> handlePaymentRefunded(PaymentEvent event);

    Mono<Void> handlePaymentPartiallyRefunded(PaymentEvent event);

    Mono<Void> handleReservationExpired(ReservationExpiredEvent event);

    Mono<Void> handleOrderCancelled(OrderEvent event);

    enum SagaState {
        ORDER_CREATED,
        INVENTORY_RESERVED,
        INVENTORY_PARTIALLY_RESERVED,
        INVENTORY_FAILED,
        PAYMENT_AUTHORIZED,
        PAYMENT_CAPTURED,
        PAYMENT_FAILED,
        ORDER_CONFIRMED,
        ORDER_CANCELLED,
        REFUNDED,
        PARTIALLY_REFUNDED
    }
}