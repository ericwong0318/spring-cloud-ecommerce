package com.example.order.repository;

import com.example.order.model.OrderItem;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface OrderItemRepository extends R2dbcRepository<OrderItem, Long> {
    Flux<OrderItem> findByOrderId(Long orderId);
    Flux<OrderItem> findByOrderIdAndStatus(Long orderId, OrderItem.OrderItemStatus status);
    Mono<OrderItem> findByVariantIdAndStatus(Long variantId, OrderItem.OrderItemStatus status);
    Flux<OrderItem> findByStatusAndReservedAtBefore(OrderItem.OrderItemStatus status, LocalDateTime cutoff);
}