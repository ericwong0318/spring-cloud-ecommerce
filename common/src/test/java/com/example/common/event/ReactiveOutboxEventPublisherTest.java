package com.example.common.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReactiveOutboxEventPublisherTest {

    @Mock ReactiveOutboxEventRepository<OutboxEvent> repository;
    @Mock RabbitTemplate rabbitTemplate;
    @Mock ObjectMapper objectMapper;
    @Mock R2dbcTransactionManager transactionManager;
    @Mock TransactionalOperator transactionalOperator;

    private ReactiveOutboxEventPublisher<OutboxEvent> publisher;

    @BeforeEach
    void setUp() {
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        properties.setMaxRetries(5);
        properties.setBatchSize(10);
        properties.setExchange("outbox.exchange");
        RoutingKeyStrategy routingKeyStrategy = RoutingKeyStrategy.DEFAULT;

        publisher = new ReactiveOutboxEventPublisher<>(
                repository, rabbitTemplate, objectMapper, transactionalOperator,
                properties, routingKeyStrategy);

        // Mock TransactionalOperator to execute the callback immediately
        doAnswer(inv -> {
            Mono<?> callback = inv.getArgument(0);
            return callback.then();
        }).when(transactionalOperator).transactional(any(Mono.class));
    }

    @Test
    void saveEvent_shouldSerializeAndSaveInTransaction() throws Exception {
        // Arrange
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"id\":\"1\"}");
        when(repository.save(any(OutboxEvent.class))).thenReturn(Mono.empty());

        // Act
        publisher.saveEvent("Order", "1", "ORDER_CREATED", new TestPayload("test")).block();

        // Assert
        verify(objectMapper).writeValueAsString(any());
        verify(repository).save(argThat(e ->
                e.getAggregateType().equals("Order") &&
                e.getAggregateId().equals("1") &&
                e.getEventType().equals("ORDER_CREATED") &&
                e.getPayload().equals("{\"id\":\"1\"}")
        ));
        verify(transactionalOperator).transactional(any(Mono.class));
    }

    @Test
    void publishOutboxEventsReactive_shouldPublishAndMarkPublished() {
        // Arrange
        OutboxEvent event = new OutboxEvent("Order", "1", "ORDER_CREATED", "{\"id\":\"1\"}");
        when(repository.findUnpublishedEventsWithRetryLimit(anyInt())).thenReturn(Flux.just(event));
        when(repository.save(any(OutboxEvent.class))).thenReturn(Mono.empty());
        doNothing().when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        // Act & Assert
        StepVerifier.create(publisher.publishOutboxEventsReactive())
                .verifyComplete();

        verify(repository).findUnpublishedEventsWithRetryLimit(5);
        verify(rabbitTemplate).convertAndSend(eq("outbox.exchange"), eq("order.order_created"), eq("{\"id\":\"1\"}"));
        verify(repository).save(argThat(e -> e.getPublishedAt() != null));
        verify(transactionalOperator).transactional(any(Mono.class));
    }

    @Test
    void publishOutboxEventsReactive_shouldIncrementRetryOnFailure() {
        // Arrange
        OutboxEvent event = new OutboxEvent("Order", "1", "ORDER_CREATED", "{\"id\":\"1\"}");
        when(repository.findUnpublishedEventsWithRetryLimit(anyInt())).thenReturn(Flux.just(event));
        when(repository.save(any(OutboxEvent.class))).thenReturn(Mono.empty());
        doThrow(new RuntimeException("RabbitMQ down")).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        // Act & Assert
        StepVerifier.create(publisher.publishOutboxEventsReactive())
                .verifyComplete();

        verify(repository).save(argThat(e -> e.getRetryCount() == 1));
    }

    private record TestPayload(String value) {}
}