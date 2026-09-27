package com.example.commonsaga;

import com.example.common.event.InventoryEvent;
import com.example.common.event.OrderEvent;
import com.example.common.event.PaymentEvent;
import com.example.common.event.ReservationExpiredEvent;
import reactor.core.publisher.Mono;

public interface OrderSagaHandler {

    Mono<Void> handleInventoryReserved(InventoryEvent event);

    Mono<Void> handleInventoryReleased(InventoryEvent event);

    Mono<Void> handlePaymentAuthorized(PaymentEvent event);

    Mono<Void> handlePaymentCaptured(PaymentEvent event);

    Mono<Void> handlePaymentFailed(PaymentEvent event);

    Mono<Void> handlePaymentRefunded(PaymentEvent event);

    Mono<Void> handlePaymentPartiallyRefunded(PaymentEvent event);

    Mono<Void> handleReservationExpired(ReservationExpiredEvent event);

    Mono<Void> handleOrderCancelled(OrderEvent event);
}