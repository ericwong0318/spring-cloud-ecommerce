package com.example.commonsaga;

import com.example.common.event.InventoryEvent;
import com.example.common.event.PaymentEvent;
import reactor.core.publisher.Mono;

public interface PaymentSagaHandler {

    Mono<Void> handleInventoryReserved(InventoryEvent event);

    Mono<Void> handlePaymentAuthorized(PaymentEvent event);

    Mono<Void> handlePaymentCaptured(PaymentEvent event);

    Mono<Void> handlePaymentFailed(PaymentEvent event);

    Mono<Void> handlePaymentRefunded(PaymentEvent event);

    Mono<Void> handlePaymentPartiallyRefunded(PaymentEvent event);
}