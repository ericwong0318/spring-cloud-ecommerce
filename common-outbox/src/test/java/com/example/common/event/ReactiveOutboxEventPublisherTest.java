package com.example.common.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReactiveOutboxEventPublisherTest {

    @Mock
    ReactiveOutboxEventRepository<OutboxEvent> repository;

    @Mock
    RabbitTemplate rabbitTemplate;

    @Mock
    ObjectMapper objectMapper;

    @Mock
    TransactionalOperator transactionalOperator;

    @Mock
    OutboxPublisherProperties properties;

    @Mock
    RoutingKeyStrategy routingKeyStrategy;

    private ReactiveOutboxEventPublisher<OutboxEvent> publisher() {
        return new ReactiveOutboxEventPublisher<>(
                repository, rabbitTemplate, objectMapper,
                transactionalOperator, properties, routingKeyStrategy
        );
    }

    @Test
    void saveEvent_shouldSerializePayloadAndSaveEventWithinTransaction() throws Exception {
        // Arrange
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"orderId\":\"1\"}");
        when(transactionalOperator.transactional(any(Mono.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repository.save(any(OutboxEvent.class))).thenReturn(Mono.just(new OutboxEvent()));

        // Act
        Mono<Void> result = publisher().saveEvent("Order", "order-1", "ORDER_CREATED", new Object());

        // Assert
        StepVerifier.create(result).verifyComplete();

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getAggregateType()).isEqualTo("Order");
        assertThat(captor.getValue().getAggregateId()).isEqualTo("order-1");
        assertThat(captor.getValue().getEventType()).isEqualTo("ORDER_CREATED");
        assertThat(captor.getValue().getPayload()).isEqualTo("{\"orderId\":\"1\"}");
    }

    @Test
    void publishOutboxEventsReactive_shouldPublishEachUnpublishedEventAndMarkPublished() throws Exception {
        // Arrange
        OutboxEvent event = new OutboxEvent("Order", "order-1", "ORDER_CREATED", "{\"orderId\":\"1\"}");
        when(properties.getMaxRetries()).thenReturn(5);
        when(properties.getBatchSize()).thenReturn(100);
        when(properties.getExchange()).thenReturn("outbox.exchange");
        when(repository.findUnpublishedEventsWithRetryLimit(5)).thenReturn(Flux.just(event));
        when(routingKeyStrategy.determineRoutingKey("Order", "ORDER_CREATED")).thenReturn("order.order_created");
        when(transactionalOperator.transactional(any(Mono.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repository.save(any(OutboxEvent.class))).thenReturn(Mono.just(event));

        // Act & Assert
        StepVerifier.create(publisher().publishOutboxEventsReactive())
                .verifyComplete();

        verify(rabbitTemplate).convertAndSend("outbox.exchange", "order.order_created", "{\"orderId\":\"1\"}");
        assertThat(event.getPublishedAt()).isNotNull();
        verify(repository).save(event);
    }

    @Test
    void publishOutboxEventsReactive_withNoEvents_shouldCompleteWithoutPublishing() {
        // Arrange
        when(properties.getMaxRetries()).thenReturn(5);
        when(properties.getBatchSize()).thenReturn(100);
        when(repository.findUnpublishedEventsWithRetryLimit(5)).thenReturn(Flux.empty());

        // Act & Assert
        StepVerifier.create(publisher().publishOutboxEventsReactive())
                .verifyComplete();

        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void determineRoutingKey_delegatesToRoutingKeyStrategy() {
        // Arrange
        when(routingKeyStrategy.determineRoutingKey("Order", "ORDER_CREATED")).thenReturn("order.order_created");

        // Act
        String key = publisher().determineRoutingKey("Order", "ORDER_CREATED");

        // Assert
        assertThat(key).isEqualTo("order.order_created");
    }
}
