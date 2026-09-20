package com.example.order.listener;

import com.example.common.event.BaseEvent;
import com.example.common.event.ReservationExpiredEvent;
import com.example.common.event.ReactiveIdempotentEventProcessor;
import com.example.order.service.OrderSagaOrchestrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Function;

import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationExpiredEventListenerTest {

    @Mock
    private ReactiveIdempotentEventProcessor idempotentEventProcessor;

    @Mock
    private OrderSagaOrchestrator sagaOrchestrator;

    private ReservationExpiredEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new ReservationExpiredEventListener(idempotentEventProcessor, sagaOrchestrator);
    }

    private ReservationExpiredEvent createReservationExpiredEvent(Long orderItemId, Long variantId) {
        ReservationExpiredEvent event = new ReservationExpiredEvent();
        ReflectionTestUtils.setField(event, "eventType", "EXPIRED");
        ReflectionTestUtils.setField(event, "eventId", UUID.randomUUID());
        ReflectionTestUtils.setField(event, "orderItemId", orderItemId);
        ReflectionTestUtils.setField(event, "variantId", variantId);
        ReflectionTestUtils.setField(event, "quantityReleased", 2);
        ReflectionTestUtils.setField(event, "reservationExpiresAt", LocalDateTime.now());
        return event;
    }

    private void mockIdempotentProcessor(ReservationExpiredEvent event) {
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Function<ReservationExpiredEvent, Mono<Void>> handler = invocation.getArgument(1);
            return handler.apply(event);
        }).when(idempotentEventProcessor).process(any(BaseEvent.class), any(Function.class));
    }

    @Test
    void handleReservationExpiredEvent_shouldDelegateToSagaOrchestrator() {
        ReservationExpiredEvent event = createReservationExpiredEvent(1L, 1L);

        when(sagaOrchestrator.handleReservationExpired(event)).thenReturn(Mono.empty());

        mockIdempotentProcessor(event);

        listener.handleReservationExpiredEvent(event);

        verify(sagaOrchestrator).handleReservationExpired(event);
    }

    @Test
    void handleReservationExpiredEvent_shouldSkipDuplicateEvent() {
        ReservationExpiredEvent event = createReservationExpiredEvent(1L, 1L);

        doAnswer(invocation -> Mono.empty()).when(idempotentEventProcessor).process(any(BaseEvent.class), any(Function.class));

        listener.handleReservationExpiredEvent(event);

        verify(sagaOrchestrator, never()).handleReservationExpired(any());
    }
}