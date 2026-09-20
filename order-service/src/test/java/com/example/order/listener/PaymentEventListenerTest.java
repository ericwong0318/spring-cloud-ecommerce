package com.example.order.listener;

import com.example.common.event.BaseEvent;
import com.example.common.event.PaymentEvent;
import com.example.common.event.ReactiveIdempotentEventProcessor;
import com.example.order.service.OrderSagaOrchestrator;
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
    private OrderSagaOrchestrator sagaOrchestrator;

    private PaymentEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new PaymentEventListener(idempotentEventProcessor, sagaOrchestrator);
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
    void handlePaymentEvent_shouldDelegateToSagaOrchestrator_whenCaptured() {
        PaymentEvent event = createPaymentEvent("CAPTURED", PaymentEvent.PaymentStatus.CAPTURED);

        when(sagaOrchestrator.handlePaymentCaptured(event)).thenReturn(Mono.empty());

        mockIdempotentProcessor(event);

        listener.handlePaymentEvent(event);

        verify(sagaOrchestrator).handlePaymentCaptured(event);
    }

    @Test
    void handlePaymentEvent_shouldDelegateToSagaOrchestrator_whenFailed() {
        PaymentEvent event = createPaymentEvent("FAILED", PaymentEvent.PaymentStatus.FAILED);

        when(sagaOrchestrator.handlePaymentFailed(event)).thenReturn(Mono.empty());

        mockIdempotentProcessor(event);

        listener.handlePaymentEvent(event);

        verify(sagaOrchestrator).handlePaymentFailed(event);
    }

    @Test
    void handlePaymentEvent_shouldDelegateToSagaOrchestrator_whenRefunded() {
        PaymentEvent event = createPaymentEvent("REFUNDED", PaymentEvent.PaymentStatus.REFUNDED);

        when(sagaOrchestrator.handlePaymentRefunded(event)).thenReturn(Mono.empty());

        mockIdempotentProcessor(event);

        listener.handlePaymentEvent(event);

        verify(sagaOrchestrator).handlePaymentRefunded(event);
    }

    @Test
    void handlePaymentEvent_shouldDelegateToSagaOrchestrator_whenPartiallyRefunded() {
        PaymentEvent event = createPaymentEvent("REFUNDED", PaymentEvent.PaymentStatus.PARTIALLY_REFUNDED);

        when(sagaOrchestrator.handlePaymentPartiallyRefunded(event)).thenReturn(Mono.empty());

        mockIdempotentProcessor(event);

        listener.handlePaymentEvent(event);

        verify(sagaOrchestrator).handlePaymentPartiallyRefunded(event);
    }

    @Test
    void handlePaymentEvent_shouldDelegateToSagaOrchestrator_whenAuthorized() {
        PaymentEvent event = createPaymentEvent("AUTHORIZED", PaymentEvent.PaymentStatus.AUTHORIZED);

        when(sagaOrchestrator.handlePaymentAuthorized(event)).thenReturn(Mono.empty());

        mockIdempotentProcessor(event);

        listener.handlePaymentEvent(event);

        verify(sagaOrchestrator).handlePaymentAuthorized(event);
    }

    @Test
    void handlePaymentEvent_shouldSkipDuplicateEvent() {
        PaymentEvent event = createPaymentEvent("CAPTURED", PaymentEvent.PaymentStatus.CAPTURED);

        doAnswer(invocation -> Mono.empty()).when(idempotentEventProcessor).process(any(), any());

        listener.handlePaymentEvent(event);

        verify(sagaOrchestrator, never()).handlePaymentCaptured(any());
    }

    @Test
    void handlePaymentEvent_shouldSkip_whenOrderIdMissing() {
        PaymentEvent event = createPaymentEvent("CAPTURED", PaymentEvent.PaymentStatus.CAPTURED);
        ReflectionTestUtils.setField(event, "orderId", null);

        doAnswer(invocation -> Mono.empty()).when(idempotentEventProcessor).process(any(), any());

        listener.handlePaymentEvent(event);

        verify(sagaOrchestrator, never()).handlePaymentCaptured(any());
    }
}