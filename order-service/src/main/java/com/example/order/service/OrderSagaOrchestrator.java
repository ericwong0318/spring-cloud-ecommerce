package com.example.order.service;

import com.example.common.dto.OrderDto;
import com.example.common.event.OrderEvent;
import reactor.core.publisher.Mono;

public interface OrderSagaOrchestrator {

    Mono<OrderDto> createOrder(OrderDto orderDto);

    Mono<Void> handleReservationExpiry(Long orderId);
}