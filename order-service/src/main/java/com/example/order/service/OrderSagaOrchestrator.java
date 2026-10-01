package com.example.order.service;

import com.example.common.dto.CreateOrderRequest;
import com.example.common.dto.OrderDto;
import com.example.common.event.InventoryEvent;
import com.example.common.event.OrderEvent;
import com.example.common.event.PaymentEvent;
import com.example.common.event.ReservationExpiredEvent;
import com.example.common.saga.SagaOrchestrator;
import reactor.core.publisher.Mono;

public interface OrderSagaOrchestrator extends SagaOrchestrator {

    @Override
    Mono<OrderDto> createOrder(CreateOrderRequest request);

    @Override
    Mono<Void> handleOrderCreated(OrderEvent event);

    @Override
    Mono<Void> handleInventoryReserved(InventoryEvent event);

    @Override
    Mono<Void> handleInventoryReleased(InventoryEvent event);

    @Override
    Mono<Void> handlePaymentAuthorized(PaymentEvent event);

    @Override
    Mono<Void> handlePaymentCaptured(PaymentEvent event);

    @Override
    Mono<Void> handlePaymentFailed(PaymentEvent event);

    @Override
    Mono<Void> handlePaymentRefunded(PaymentEvent event);

    @Override
    Mono<Void> handlePaymentPartiallyRefunded(PaymentEvent event);

    @Override
    Mono<Void> handleReservationExpired(ReservationExpiredEvent event);

    @Override
    Mono<Void> handleOrderCancelled(OrderEvent event);

    Mono<Void> handleReservationExpiry(Long orderId);
}