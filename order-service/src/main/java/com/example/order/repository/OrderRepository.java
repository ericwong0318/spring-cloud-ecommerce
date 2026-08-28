package com.example.order.repository;

import com.example.order.model.Order;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OrderRepository extends ReactiveCrudRepository<Order, Long> {
    Flux<Order> findByCustomerId(String customerId);
    Mono<Order> findByIdAndCustomerId(Long id, String customerId);
    Flux<Order> findByStatus(String status);
}