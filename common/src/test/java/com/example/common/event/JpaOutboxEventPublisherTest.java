package com.example.common.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.TransactionStatus;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JpaOutboxEventPublisherTest {

    @Mock OutboxEventRepository repository;
    @Mock RabbitTemplate rabbitTemplate;
    @Mock ObjectMapper objectMapper;
    @Mock TransactionTemplate transactionTemplate;
    @Mock TransactionStatus transactionStatus;

    private JpaOutboxEventPublisher publisher;

    @BeforeEach
    void setUp() {
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        properties.setMaxRetries(5);
        properties.setBatchSize(10);
        properties.setExchange("outbox.exchange");
        RoutingKeyStrategy routingKeyStrategy = RoutingKeyStrategy.DEFAULT;

        publisher = new JpaOutboxEventPublisher(
                repository, rabbitTemplate, objectMapper, transactionTemplate,
                properties, routingKeyStrategy);

        // Mock TransactionTemplate to execute the callback immediately
        doAnswer(inv -> {
            TransactionCallback<?> callback = inv.getArgument(0);
            return callback.doInTransaction(transactionStatus);
        }).when(transactionTemplate).execute(any(TransactionCallback.class));
    }

    @Test
    void saveEvent_shouldSerializeAndSaveInTransaction() throws Exception {
        // Arrange
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"id\":\"1\"}");
        doNothing().when(repository).save(any(OutboxEvent.class));

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
        verify(transactionTemplate).execute(any(TransactionCallback.class));
    }

    @Test
    void publishOutboxEvents_shouldPublishAndMarkPublished() {
        // Arrange
        OutboxEvent event = new OutboxEvent("Order", "1", "ORDER_CREATED", "{\"id\":\"1\"}");
        when(repository.findUnpublishedEventsWithRetryLimit(anyInt())).thenReturn(List.of(event));
        doNothing().when(repository).save(any(OutboxEvent.class));
        doNothing().when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        // Act
        publisher.publishOutboxEvents();

        // Assert
        verify(repository).findUnpublishedEventsWithRetryLimit(5);
        verify(rabbitTemplate).convertAndSend(eq("outbox.exchange"), eq("order.order.created"), eq("{\"id\":\"1\"}"));
        verify(repository).save(argThat(e -> e.getPublishedAt() != null));
        verify(transactionTemplate, times(1)).execute(any(TransactionCallback.class));
    }

    private record TestPayload(String value) {}
}
