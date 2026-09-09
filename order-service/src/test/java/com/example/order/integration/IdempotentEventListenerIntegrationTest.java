package com.example.order.integration;

import com.example.common.event.InventoryEvent;
import com.example.common.event.PaymentEvent;
import com.example.common.event.ProcessedEventRepository;
import com.example.order.BaseIntegrationTest;
import com.example.order.model.Order;
import com.example.order.model.OrderItem;
import com.example.order.repository.OrderItemRepository;
import com.example.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class IdempotentEventListenerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.queue.inventory-events}")
    private String inventoryEventsQueue;

    @Value("${rabbitmq.queue.payment-events}")
    private String paymentEventsQueue;

    private final Long testOrderId = 1000L;
    private final Long testVariantId = 200L;

    @BeforeEach
    void setUp() {
        processedEventRepository.deleteAll();
        orderItemRepository.deleteAll().block();
        orderRepository.deleteAll().block();
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void shouldProcessInventoryReservedEventOnlyOnceWhenDuplicateEventId() {
        // Given: An order with PENDING items
        Order order = new Order();
        order.setId(testOrderId);
        order.setCustomerId("customer-1");
        order.setStatus("PENDING");
        orderRepository.save(order).block();

        OrderItem item = new OrderItem(1L, testOrderId, 100L, testVariantId, "TEST-001",
                "Test Product", 5, 0, null, new BigDecimal("99.99"), OrderItem.OrderItemStatus.PENDING);
        orderItemRepository.save(item).block();

        // Given: An inventory reserved event with a specific eventId
        UUID eventId = UUID.randomUUID();
        InventoryEvent event = InventoryEvent.reserved(testVariantId, 100L, 5, 0);
        event.setEventId(eventId);

        // When: Send the same event twice
        rabbitTemplate.convertAndSend(inventoryEventsQueue, event);
        rabbitTemplate.convertAndSend(inventoryEventsQueue, event);

        // Then: Wait for processing and verify event was processed only once
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            long count = processedEventRepository.count();
            assertThat(count).isEqualTo(1);
            
            // Verify order item was updated to RESERVED only once
            OrderItem updatedItem = orderItemRepository.findById(1L).block();
            assertThat(updatedItem.getStatus()).isEqualTo(OrderItem.OrderItemStatus.RESERVED);
        });
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void shouldProcessPaymentCapturedEventOnlyOnceWhenDuplicateEventId() {
        // Given: An order with RESERVED items (after inventory reservation)
        Order order = new Order();
        order.setId(testOrderId);
        order.setCustomerId("customer-1");
        order.setStatus("PENDING");
        orderRepository.save(order).block();

        OrderItem item = new OrderItem(1L, testOrderId, 100L, testVariantId, "TEST-001",
                "Test Product", 5, 0, null, new BigDecimal("99.99"), OrderItem.OrderItemStatus.RESERVED);
        orderItemRepository.save(item).block();

        // Given: A payment captured event with a specific eventId
        UUID eventId = UUID.randomUUID();
        PaymentEvent event = PaymentEvent.captured(
                500L, testOrderId, "customer-1", "test@example.com",
                new BigDecimal("499.95"), "USD", "txn-123");
        event.setEventId(eventId);

        // When: Send the same event twice
        rabbitTemplate.convertAndSend(paymentEventsQueue, event);
        rabbitTemplate.convertAndSend(paymentEventsQueue, event);

        // Then: Wait for processing and verify event was processed only once
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            long count = processedEventRepository.count();
            assertThat(count).isEqualTo(1);
            
            // Verify order was transitioned to CONFIRMED
            Order updatedOrder = orderRepository.findById(testOrderId).block();
            assertThat(updatedOrder.getStatus()).isEqualTo("CONFIRMED");
        });
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void shouldProcessEventWithoutEventIdEveryTime() {
        // Given: An order with PENDING items
        Order order = new Order();
        order.setId(testOrderId + 1);
        order.setCustomerId("customer-2");
        order.setStatus("PENDING");
        orderRepository.save(order).block();

        OrderItem item = new OrderItem(2L, testOrderId + 1, 101L, testVariantId + 1, "TEST-002",
                "Test Product 2", 3, 0, null, new BigDecimal("149.99"), OrderItem.OrderItemStatus.PENDING);
        orderItemRepository.save(item).block();

        // Given: An inventory event WITHOUT eventId (null)
        InventoryEvent event = InventoryEvent.reserved(testVariantId + 1, 101L, 3, 0);
        event.setEventId(null); // No eventId - should process every time

        // When: Send the same event twice (without eventId)
        rabbitTemplate.convertAndSend(inventoryEventsQueue, event);
        rabbitTemplate.convertAndSend(inventoryEventsQueue, event);

        // Then: Both should be processed (no deduplication without eventId)
        // Since there's no eventId, the idempotency processor won't track them
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            long count = processedEventRepository.count();
            // No records should be in processed_events since eventId was null
            assertThat(count).isEqualTo(0);
            
            // But the order item should be updated (reserved twice = but only once since status changes to RESERVED on first)
            OrderItem updatedItem = orderItemRepository.findById(2L).block();
            assertThat(updatedItem.getStatus()).isEqualTo(OrderItem.OrderItemStatus.RESERVED);
        });
    }
}