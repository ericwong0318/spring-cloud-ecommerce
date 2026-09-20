package com.example.order.listener;

import com.example.common.event.BaseEvent;
import com.example.common.event.InventoryEvent;
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
class InventoryEventListenerTest {

    @Mock
    private ReactiveIdempotentEventProcessor idempotentEventProcessor;

    @Mock
    private OrderSagaOrchestrator sagaOrchestrator;

    private InventoryEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new InventoryEventListener(idempotentEventProcessor, sagaOrchestrator);
    }

    private InventoryEvent createInventoryEvent(String eventType, Long variantId, int reserved, int backordered) {
        InventoryEvent event = new InventoryEvent();
        ReflectionTestUtils.setField(event, "eventType", eventType);
        ReflectionTestUtils.setField(event, "eventId", UUID.randomUUID());
        ReflectionTestUtils.setField(event, "variantId", variantId);
        ReflectionTestUtils.setField(event, "productId", 1L);
        ReflectionTestUtils.setField(event, "productName", "Test Product");
        ReflectionTestUtils.setField(event, "reserved", reserved);
        ReflectionTestUtils.setField(event, "backordered", backordered);
        ReflectionTestUtils.setField(event, "timestamp", LocalDateTime.now());
        return event;
    }

    private void mockIdempotentProcessor(InventoryEvent event) {
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Function<InventoryEvent, Mono<Void>> handler = invocation.getArgument(1);
            return handler.apply(event);
        }).when(idempotentEventProcessor).process(any(BaseEvent.class), any(Function.class));
    }

    @Test
    void handleInventoryEvent_shouldDelegateToSagaOrchestrator_whenReserved() {
        InventoryEvent event = createInventoryEvent("RESERVED", 1L, 2, 0);

        when(sagaOrchestrator.handleInventoryReserved(event)).thenReturn(Mono.empty());

        mockIdempotentProcessor(event);

        listener.handleInventoryEvent(event);

        verify(sagaOrchestrator).handleInventoryReserved(event);
    }

    @Test
    void handleInventoryEvent_shouldDelegateToSagaOrchestrator_whenReleased() {
        InventoryEvent event = createInventoryEvent("RELEASED", 1L, 2, 0);

        when(sagaOrchestrator.handleInventoryReleased(event)).thenReturn(Mono.empty());

        mockIdempotentProcessor(event);

        listener.handleInventoryEvent(event);

        verify(sagaOrchestrator).handleInventoryReleased(event);
    }

    @Test
    void handleInventoryEvent_shouldDelegateToSagaOrchestrator_whenConfirmed() {
        InventoryEvent event = createInventoryEvent("CONFIRMED", 1L, 2, 0);

        mockIdempotentProcessor(event);

        listener.handleInventoryEvent(event);

        verify(sagaOrchestrator, never()).handleInventoryReserved(any());
        verify(sagaOrchestrator, never()).handleInventoryReleased(any());
    }

    @Test
    void handleInventoryEvent_shouldSkip_whenVariantIdIsNull() {
        InventoryEvent event = createInventoryEvent("RESERVED", null, 2, 0);

        mockIdempotentProcessor(event);

        listener.handleInventoryEvent(event);

        verify(sagaOrchestrator, never()).handleInventoryReserved(any());
        verify(sagaOrchestrator, never()).handleInventoryReleased(any());
    }

    @Test
    void handleInventoryEvent_shouldSkipDuplicateEvent() {
        InventoryEvent event = createInventoryEvent("RESERVED", 1L, 2, 0);

        doAnswer(invocation -> Mono.empty()).when(idempotentEventProcessor).process(any(BaseEvent.class), any(Function.class));

        listener.handleInventoryEvent(event);

        verify(sagaOrchestrator, never()).handleInventoryReserved(any());
    }
}