package com.example.order.service;

import com.example.common.event.OrderEvent;
import com.example.order.model.Order;

import java.math.BigDecimal;

import reactor.core.publisher.Mono;

public interface OrderEventPublisher {

    Mono<Void> publishOrderCreated(Order order);

    Mono<Void> publishOrderUpdated(Order order);

    Mono<Void> publishOrderCancelled(Order order);

    Mono<Void> publishOrderConfirmed(Order order, BigDecimal amount);

    Mono<Void> publishPaymentAuthorized(Order order);

    Mono<Void> publishPaymentRefunded(Order order);

    Mono<Void> publishPaymentPartiallyRefunded(Order order);

    Mono<Void> publishOrderEvent(OrderEvent event);
}