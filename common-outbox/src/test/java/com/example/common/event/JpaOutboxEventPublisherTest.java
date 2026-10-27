package com.example.common.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JpaOutboxEventPublisherTest {

    @Mock
    OutboxEventRepository repository;

    @Mock
    RabbitTemplate rabbitTemplate;

    @Mock
    ObjectMapper objectMapper;

    @Mock
    TransactionTemplate transactionTemplate;

    @Mock
    OutboxPublisherProperties properties;

    @Mock
    RoutingKeyStrategy routingKeyStrategy;

    private JpaOutboxEventPublisher publisher() {
        return new JpaOutboxEventPublisher(
                repository, rabbitTemplate, objectMapper,
                transactionTemplate, properties, routingKeyStrategy
        );
    }

    @Test
    void saveEvent_shouldSerializePayloadAndSaveEventWithinTransaction() throws Exception {
        // Arrange
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"orderId\":\"1\"}");
        when(transactionTemplate.execute(any(TransactionCallback.class)))
                .thenAnswer(inv -> ((TransactionCallback<Object>) inv.getArgument(0)).doInTransaction(mock(TransactionStatus.class)));
        when(repository.save(any(OutboxEvent.class))).thenReturn(new OutboxEvent());

        // Act
        Mono<Void> result = publisher().saveEvent("Order", "order-1", "ORDER_CREATED", new Object());

        // Assert
        result.block();

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getAggregateType()).isEqualTo("Order");
        assertThat(captor.getValue().getAggregateId()).isEqualTo("order-1");
        assertThat(captor.getValue().getEventType()).isEqualTo("ORDER_CREATED");
        assertThat(captor.getValue().getPayload()).isEqualTo("{\"orderId\":\"1\"}");
    }

    @Test
    void publishOutboxEvents_shouldPublishEachUnpublishedEventAndMarkPublished() throws Exception {
        // Arrange
        OutboxEvent event = new OutboxEvent("Order", "order-1", "ORDER_CREATED", "{\"orderId\":\"1\"}");
        when(properties.getMaxRetries()).thenReturn(5);
        when(properties.getBatchSize()).thenReturn(100);
        when(properties.getExchange()).thenReturn("outbox.exchange");
        when(repository.findUnpublishedEventsWithRetryLimit(5)).thenReturn(List.of(event));
        when(routingKeyStrategy.determineRoutingKey("Order", "ORDER_CREATED")).thenReturn("order.order_created");
        when(transactionTemplate.execute(any(TransactionCallback.class)))
                .thenAnswer(inv -> ((TransactionCallback<Object>) inv.getArgument(0)).doInTransaction(mock(TransactionStatus.class)));

        // Act
        publisher().publishOutboxEvents();

        // Assert
        verify(rabbitTemplate).convertAndSend("outbox.exchange", "order.order_created", "{\"orderId\":\"1\"}");
        assertThat(event.getPublishedAt()).isNotNull();
        verify(repository).save(event);
    }

    @Test
    void publishOutboxEvents_withNoEvents_shouldCompleteWithoutPublishing() {
        // Arrange
        when(properties.getMaxRetries()).thenReturn(5);
        when(repository.findUnpublishedEventsWithRetryLimit(5)).thenReturn(List.of());

        // Act
        publisher().publishOutboxEvents();

        // Assert
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void publishOutboxEvents_limitsToBatchSize() {
        // Arrange
        OutboxEvent e1 = new OutboxEvent("Order", "order-1", "ORDER_CREATED", "{}");
        OutboxEvent e2 = new OutboxEvent("Order", "order-2", "ORDER_CREATED", "{}");
        OutboxEvent e3 = new OutboxEvent("Order", "order-3", "ORDER_CREATED", "{}");
        when(properties.getMaxRetries()).thenReturn(5);
        when(properties.getBatchSize()).thenReturn(2);
        when(properties.getExchange()).thenReturn("outbox.exchange");
        when(repository.findUnpublishedEventsWithRetryLimit(5)).thenReturn(List.of(e1, e2, e3));
        when(routingKeyStrategy.determineRoutingKey(anyString(), anyString()))
                .thenAnswer(inv -> ((String) inv.getArgument(0)).toLowerCase() + "." + ((String) inv.getArgument(1)).toLowerCase());
        when(transactionTemplate.execute(any(TransactionCallback.class)))
                .thenAnswer(inv -> ((TransactionCallback<Object>) inv.getArgument(0)).doInTransaction(mock(TransactionStatus.class)));

        // Act
        publisher().publishOutboxEvents();

        // Assert - only 2 events published due to batch size limit
        verify(rabbitTemplate, times(2)).convertAndSend(anyString(), anyString(), anyString());
        assertThat(e1.getPublishedAt()).isNotNull();
        assertThat(e2.getPublishedAt()).isNotNull();
        assertThat(e3.getPublishedAt()).isNull();
    }
}
