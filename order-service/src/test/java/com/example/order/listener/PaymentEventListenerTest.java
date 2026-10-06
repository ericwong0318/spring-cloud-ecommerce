package com.example.order.listener;

import com.example.common.event.BaseEvent;
import com.example.common.event.PaymentEvent;
import com.example.common.event.ReactiveIdempotentEventProcessor;
import com.example.commonsaga.OrderSagaHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Function;

import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentEventListenerTest {

    @Mock
    private ReactiveIdempotentEventProcessor idempotentEventProcessor;

    @Mock
    private OrderSagaHandler sagaHandler;

    private PaymentEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new PaymentEventListener(idempotentEventProcessor, sagaHandler);
    }

    private PaymentEvent createPaymentEvent(String eventType, PaymentEvent.PaymentStatus status) {
        PaymentEvent event = new PaymentEvent();
        ReflectionTestUtils.setField(event, "eventType", eventType);
        ReflectionTestUtils.setField(event, "eventId", UUID.randomUUID());
        ReflectionTestUtils.setField(event, "orderId", 1L);
        ReflectionTestUtils.setField(event, "paymentId", 100L);
        ReflectionTestUtils.setField(event, "customerId", "CUST-001");
        ReflectionTestUtils.setField(event, "customerEmail", "customer@example.com");
        ReflectionTestUtils.setField(event, "amount", new BigDecimal("1999.98"));
        ReflectionTestUtils.setField(event, "currency", "USD");
        ReflectionTestUtils.setField(event, "status", status);
        ReflectionTestUtils.setField(event, "transactionId", "txn_123456");
        ReflectionTestUtils.setField(event, "timestamp", LocalDateTime.now());
        return event;
    }

    private void mockIdempotentProcessor(PaymentEvent event) {
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Function<PaymentEvent, reactor.core.publisher.Mono<Void>> handler = invocation.getArgument(1);
            return handler.apply(event);
        }).when(idempotentEventProcessor).process(any(), any());
    }

    @Test
    void handlePaymentEvent_shouldDelegateToSagaHandler_whenCaptured() {
        PaymentEvent event = createPaymentEvent("CAPTURED", PaymentEvent.PaymentStatus.CAPTURED);

        when(sagaHandler.handlePaymentCaptured(event)).thenReturn(Mono.empty());

        mockIdempotentProcessor(event);

        listener.handlePaymentEvent(event);

        verify(sagaHandler).handlePaymentCaptured(event);
    }

    @Test
    void handlePaymentEvent_shouldDelegateToSagaHandler_whenFailed() {
        PaymentEvent event = createPaymentEvent("FAILED", PaymentEvent.PaymentStatus.FAILED);

        when(sagaHandler.handlePaymentFailed(event)).thenReturn(Mono.empty());

        mockIdempotentProcessor(event);

        listener.handlePaymentEvent(event);

        verify(sagaHandler).handlePaymentFailed(event);
    }

    @Test
    void handlePaymentEvent_shouldDelegateToSagaHandler_whenRefunded() {
        PaymentEvent event = createPaymentEvent("REFUNDED", PaymentEvent.PaymentStatus.REFUNDED);

        when(sagaHandler.handlePaymentRefunded(event)).thenReturn(Mono.empty());

        mockIdempotentProcessor(event);

        listener.handlePaymentEvent(event);

        verify(sagaHandler).handlePaymentRefunded(event);
    }

    @Test
    void handlePaymentEvent_shouldDelegateToSagaHandler_whenPartiallyRefunded() {
        PaymentEvent event = createPaymentEvent("REFUNDED", PaymentEvent.PaymentStatus.PARTIALLY_REFUNDED);

        when(sagaHandler.handlePaymentPartiallyRefunded(event)).thenReturn(Mono.empty());

        mockIdempotentProcessor(event);

        listener.handlePaymentEvent(event);

        verify(sagaHandler).handlePaymentPartiallyRefunded(event);
    }

    @Test
    void handlePaymentEvent_shouldDelegateToSagaHandler_whenAuthorized() {
        PaymentEvent event = createPaymentEvent("AUTHORIZED", PaymentEvent.PaymentStatus.AUTHORIZED);

        when(sagaHandler.handlePaymentAuthorized(event)).thenReturn(Mono.empty());

        mockIdempotentProcessor(event);

        listener.handlePaymentEvent(event);

        verify(sagaHandler).handlePaymentAuthorized(event);
    }

    @Test
    void handlePaymentEvent_shouldSkipDuplicateEvent() {
        PaymentEvent event = createPaymentEvent("CAPTURED", PaymentEvent.PaymentStatus.CAPTURED);

        doAnswer(invocation -> Mono.empty()).when(idempotentEventProcessor).process(any(), any());

        listener.handlePaymentEvent(event);

        verify(sagaHandler, never()).handlePaymentCaptured(any());
    }

    @Test
    void handlePaymentEvent_shouldSkip_whenOrderIdMissing() {
        PaymentEvent event = createPaymentEvent("CAPTURED", PaymentEvent.PaymentStatus.CAPTURED);
        ReflectionTestUtils.setField(event, "orderId", null);

        doAnswer(invocation -> Mono.empty()).when(idempotentEventProcessor).process(any(), any());

        listener.handlePaymentEvent(event);

        verify(sagaHandler, never()).handlePaymentCaptured(any());
    }
}
