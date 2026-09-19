package com.example.common.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReactiveIdempotentEventProcessorTest {

    @Mock CommonProcessedEventRepository processedEventRepository;
    @Mock R2dbcTransactionManager transactionManager;
    @Mock TransactionalOperator transactionalOperator;

    private ReactiveIdempotentEventProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new ReactiveIdempotentEventProcessor(processedEventRepository, transactionalOperator);
    }

    @Test
    void process_shouldProcessEventWhenNotSeenBefore() {
        // given
        UUID eventId = UUID.randomUUID();
        TestEvent event = new TestEvent("TEST_EVENT", eventId);
        when(processedEventRepository.existsByEventIdReactive(eventId)).thenReturn(Mono.just(false));
        when(processedEventRepository.save(eq(eventId), any(LocalDateTime.class))).thenReturn(Mono.just(1));
        // Mock TransactionalOperator to execute the callback immediately
        doAnswer(inv -> {
            Mono<?> callback = inv.getArgument(0);
            return callback.then();
        }).when(transactionalOperator).transactional(any(Mono.class));

        // when
        StepVerifier.create(processor.process(event, e -> Mono.empty()))
                .verifyComplete();

        // then
        verify(processedEventRepository).existsByEventIdReactive(eventId);
        verify(processedEventRepository).save(eq(eventId), any(LocalDateTime.class));
        verify(transactionalOperator).transactional(any(Mono.class));
    }

    @Test
    void process_shouldSkipEventWhenAlreadyProcessed() {
        // given
        UUID eventId = UUID.randomUUID();
        TestEvent event = new TestEvent("TEST_EVENT", eventId);
        when(processedEventRepository.existsByEventIdReactive(eventId)).thenReturn(Mono.just(true));
        // Mock TransactionalOperator to execute the callback immediately
        doAnswer(inv -> {
            Mono<?> callback = inv.getArgument(0);
            return callback.then();
        }).when(transactionalOperator).transactional(any(Mono.class));

        // when
        StepVerifier.create(processor.process(event, e -> Mono.empty()))
                .verifyComplete();

        // then
        verify(processedEventRepository).existsByEventIdReactive(eventId);
        verify(processedEventRepository, never()).save(any(UUID.class), any(LocalDateTime.class));
        verify(transactionalOperator).transactional(any(Mono.class));
    }

    @Test
    void process_shouldProcessWithoutIdempotencyWhenEventIdIsNull() {
        // given
        TestEvent event = new TestEvent("TEST_EVENT", null);

        // when
        StepVerifier.create(processor.process(event, e -> Mono.empty()))
                .verifyComplete();

        // then
        verify(processedEventRepository, never()).existsByEventIdReactive(any());
        verify(processedEventRepository, never()).save(any(), any());
    }

    @Test
    void process_shouldExecuteHandlerInTransaction() {
        // given
        UUID eventId = UUID.randomUUID();
        TestEvent event = new TestEvent("TEST_EVENT", eventId);
        when(processedEventRepository.existsByEventIdReactive(eventId)).thenReturn(Mono.just(false));
        when(processedEventRepository.save(eq(eventId), any(LocalDateTime.class))).thenReturn(Mono.just(1));
        // Mock TransactionalOperator to execute the callback immediately
        doAnswer(inv -> {
            Mono<?> callback = inv.getArgument(0);
            return callback.then();
        }).when(transactionalOperator).transactional(any(Mono.class));

        // when
        StepVerifier.create(processor.process(event, e -> Mono.empty()))
                .verifyComplete();

        // then
        verify(transactionalOperator).transactional(any(Mono.class));
    }

    private record TestEvent(String eventType, UUID eventId) implements BaseEvent {
        @Override
        public UUID getEventId() {
            return eventId;
        }

        @Override
        public String getEventType() {
            return eventType;
        }
    }
}