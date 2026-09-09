package com.example.order.repository;

import com.example.order.model.Order;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface OrderRepository extends R2dbcRepository<Order, Long> {
    Flux<Order> findByCustomerId(String customerId);
    Flux<Order> findByStatus(String status);
}