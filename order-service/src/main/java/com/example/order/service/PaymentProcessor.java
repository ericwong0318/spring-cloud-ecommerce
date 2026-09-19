package com.example.order.service;

import java.math.BigDecimal;

import reactor.core.publisher.Mono;

public interface PaymentProcessor {

    Mono<Void> processRefund(Long orderId);

    Mono<Void> handlePaymentAuthorized(Long orderId);

    Mono<Void> handlePaymentCaptured(Long orderId, BigDecimal amount);

    Mono<Void> handlePaymentFailed(Long orderId);

    Mono<Void> handlePaymentRefunded(Long orderId);

    Mono<Void> handlePaymentPartiallyRefunded(Long orderId);
}