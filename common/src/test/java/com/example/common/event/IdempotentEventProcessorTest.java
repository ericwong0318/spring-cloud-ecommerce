package com.example.common.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotentEventProcessorTest {

    @Mock
    private CommonProcessedEventRepository processedEventRepository;

    private IdempotentEventProcessor idempotentEventProcessor;

    @Test
    void shouldProcessEventWhenNotSeenBefore() {
        // given
        idempotentEventProcessor = new IdempotentEventProcessor(processedEventRepository);
        OrderEvent event = OrderEvent.created(1L, "customer-1", "test@example.com", 
            java.math.BigDecimal.valueOf(100), java.util.List.of());
        when(processedEventRepository.existsByEventId(event.getEventId())).thenReturn(false);

        // when
        idempotentEventProcessor.process(event, e -> {});

        // then
        verify(processedEventRepository).existsByEventId(event.getEventId());
        verify(processedEventRepository).saveBlocking(any(ProcessedEvent.class));
    }

    @Test
    void shouldSkipEventWhenAlreadyProcessed() {
        // given
        idempotentEventProcessor = new IdempotentEventProcessor(processedEventRepository);
        OrderEvent event = OrderEvent.created(1L, "customer-1", "test@example.com",
            java.math.BigDecimal.valueOf(100), java.util.List.of());
        when(processedEventRepository.existsByEventId(event.getEventId())).thenReturn(true);

        // when
        idempotentEventProcessor.process(event, e -> {});

        // then
        verify(processedEventRepository).existsByEventId(event.getEventId());
        verify(processedEventRepository, never()).saveBlocking(any(ProcessedEvent.class));
    }

    @Test
    void shouldProcessEventWithoutIdempotencyWhenEventIdIsNull() {
        // given
        idempotentEventProcessor = new IdempotentEventProcessor(processedEventRepository);
        OrderEvent event = OrderEvent.created(1L, "customer-1", "test@example.com",
            java.math.BigDecimal.valueOf(100), java.util.List.of());
        event.setEventId(null); // eventId is null

        // when
        idempotentEventProcessor.process(event, e -> {});

        // then
        verify(processedEventRepository, never()).existsByEventId(any());
        verify(processedEventRepository, never()).saveBlocking(any());
    }
}
