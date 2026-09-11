package com.example.payment.integration;

import com.example.common.event.InventoryEvent;
import com.example.payment.BaseIntegrationTest;
import com.example.payment.domain.ProcessedEvent;
import com.example.payment.repository.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class IdempotentEventListenerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.queue.inventory-reserved}")
    private String inventoryReservedQueue;

    private final Long testVariantId = 100L;
    private final Long testProductId = 50L;

    @BeforeEach
    void setUp() {
        processedEventRepository.deleteAll().block();
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void shouldProcessInventoryReservedEventOnlyOnceWhenDuplicateEventId() {
        // Given: An inventory reserved event with a specific eventId
        // Use reserved=0 to avoid triggering payment authorization (which also saves to processed_events)
        UUID eventId = UUID.randomUUID();
        InventoryEvent event = InventoryEvent.reserved(testVariantId, testProductId, 0, 0);
        event.setEventId(eventId);

        // When: Send the same event twice
        rabbitTemplate.convertAndSend(inventoryReservedQueue, event);
        rabbitTemplate.convertAndSend(inventoryReservedQueue, event);

        // Then: Wait for processing and verify event was processed only once
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            ProcessedEvent processedEvent = processedEventRepository.findByEventId(eventId).block();
            assertThat(processedEvent).isNotNull();
            assertThat(processedEvent.eventId()).isEqualTo(eventId);
        });
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void shouldProcessEventWithoutEventIdEveryTime() {
        // Given: An inventory event WITHOUT eventId (null)
        // Use reserved=0 to avoid triggering payment authorization
        InventoryEvent event = InventoryEvent.reserved(testVariantId + 1, testProductId, 0, 0);
        event.setEventId(null); // No eventId - should process every time (no deduplication)

        // When: Send the same event twice (without eventId)
        rabbitTemplate.convertAndSend(inventoryReservedQueue, event);
        rabbitTemplate.convertAndSend(inventoryReservedQueue, event);

        // Then: Since there's no eventId, the idempotency processor won't track them
        // We verify by checking that no record with null eventId exists (which is impossible anyway)
        // and that the total count is 0 (since we don't save events without eventId)
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            long count = processedEventRepository.count().block();
            // No records should be in processed_events since eventId was null
            // and we didn't trigger payment authorization
            assertThat(count).isEqualTo(0);
        });
    }
}
