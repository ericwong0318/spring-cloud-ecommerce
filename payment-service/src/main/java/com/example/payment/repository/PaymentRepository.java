package com.example.payment.repository;

import com.example.payment.domain.Payment;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.List;

@Repository
public interface PaymentRepository extends R2dbcRepository<Payment, Long> {

    @Query("SELECT * FROM payments WHERE order_id = :orderId")
    Mono<Payment> findByOrderId(Long orderId);

    @Query("SELECT * FROM payments WHERE idempotency_key = :idempotencyKey")
    Mono<Payment> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT * FROM payments WHERE order_id = :orderId AND status = :status")
    reactor.core.publisher.Flux<Payment> findByOrderIdAndStatus(Long orderId, Payment.PaymentStatus status);

    @Query("SELECT * FROM payments WHERE order_id = :orderId")
    reactor.core.publisher.Flux<Payment> findByOrderIdAll(Long orderId);
}