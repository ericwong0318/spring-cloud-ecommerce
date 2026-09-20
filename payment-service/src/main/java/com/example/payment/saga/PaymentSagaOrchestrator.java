package com.example.payment.saga;

import com.example.common.event.InventoryEvent;
import com.example.common.event.PaymentEvent;
import reactor.core.publisher.Mono;

public interface PaymentSagaOrchestrator {

    Mono<Void> handleInventoryReserved(InventoryEvent event);

    Mono<Void> handlePaymentAuthorized(PaymentEvent event);

    Mono<Void> handlePaymentCaptured(PaymentEvent event);

    Mono<Void> handlePaymentFailed(PaymentEvent event);

    Mono<Void> handlePaymentRefunded(PaymentEvent event);
}