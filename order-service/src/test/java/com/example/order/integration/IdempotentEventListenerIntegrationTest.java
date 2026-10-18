package com.example.order.integration;

import com.example.common.event.InventoryEvent;
import com.example.common.event.PaymentEvent;
import com.example.order.BaseIntegrationTest;
import com.example.order.domain.ProcessedEvent;
import com.example.order.listener.InventoryEventListener;
import com.example.order.listener.PaymentEventListener;
import com.example.order.model.Order;
import com.example.order.model.OrderItem;
import com.example.order.repository.OrderItemRepository;
import com.example.order.repository.OrderRepository;
import com.example.order.repository.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for the idempotent event handling of the saga listeners.
 *
 * <p>The listeners are invoked directly rather than through RabbitMQ: this context
 * excludes {@code RabbitAutoConfiguration}, so there are no listener containers to
 * consume published messages. Driving the listener methods exercises the real
 * {@code ReactiveIdempotentEventProcessor} and {@code ProcessedEventRepository},
 * which is what these tests actually verify.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class IdempotentEventListenerIntegrationTest extends BaseIntegrationTest {

    private static final Long PRODUCT_ID = 100L;
    private static final Long VARIANT_ID = 200L;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private InventoryEventListener inventoryEventListener;

    @Autowired
    private PaymentEventListener paymentEventListener;

    @BeforeEach
    void setUp() {
        processedEventRepository.deleteAll().block();
        orderItemRepository.deleteAll().block();
        orderRepository.deleteAll().block();
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void shouldProcessInventoryReservedEventOnlyOnceWhenDuplicateEventId() {
        // Given: An order with a PENDING item
        Order order = persistOrder("customer-1", "PENDING");
        OrderItem item = persistItem(order.getId(), PRODUCT_ID, VARIANT_ID, 5, "99.99",
                OrderItem.OrderItemStatus.PENDING);

        // Given: an inventory reserved event with a specific eventId
        UUID eventId = UUID.randomUUID();
        InventoryEvent event = InventoryEvent.reserved(VARIANT_ID, PRODUCT_ID, 5, 0);
        event.setEventId(eventId);

        // When: the same event is delivered twice
        inventoryEventListener.handleInventoryEvent(event);
        inventoryEventListener.handleInventoryEvent(event);

        // Then: it is processed exactly once and the item becomes RESERVED
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            ProcessedEvent processedEvent = processedEventRepository.findByEventId(eventId).block();
            assertThat(processedEvent).isNotNull();
            assertThat(processedEvent.eventId()).isEqualTo(eventId);

            OrderItem updatedItem = orderItemRepository.findById(item.getId()).block();
            assertThat(updatedItem).isNotNull();
            assertThat(updatedItem.getStatus()).isEqualTo(OrderItem.OrderItemStatus.RESERVED);
        });

        assertThat(processedEventRepository.count().block())
                .as("duplicate delivery must not create a second processed-event row")
                .isEqualTo(1);
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void shouldProcessPaymentCapturedEventOnlyOnceWhenDuplicateEventId() {
        // Given: An order with a RESERVED item (after inventory reservation)
        Order order = persistOrder("customer-1", "PENDING");
        persistItem(order.getId(), PRODUCT_ID, VARIANT_ID, 5, "99.99",
                OrderItem.OrderItemStatus.RESERVED);

        // Given: a payment captured event with a specific eventId
        UUID eventId = UUID.randomUUID();
        PaymentEvent event = PaymentEvent.captured(500L, order.getId(), "customer-1",
                "test@example.com", new BigDecimal("499.95"), "USD", "txn-123");
        event.setEventId(eventId);

        // When: the same event is delivered twice
        paymentEventListener.handlePaymentEvent(event);
        paymentEventListener.handlePaymentEvent(event);

        // Then: it is processed exactly once and the order becomes CONFIRMED
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            ProcessedEvent processedEvent = processedEventRepository.findByEventId(eventId).block();
            assertThat(processedEvent).isNotNull();
            assertThat(processedEvent.eventId()).isEqualTo(eventId);

            Order updatedOrder = orderRepository.findById(order.getId()).block();
            assertThat(updatedOrder).isNotNull();
            assertThat(updatedOrder.getStatus()).isEqualTo("CONFIRMED");
        });

        assertThat(processedEventRepository.count().block())
                .as("duplicate delivery must not create a second processed-event row")
                .isEqualTo(1);
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void shouldProcessEventWithoutEventIdEveryTime() {
        // Given: An order with a PENDING item
        Order order = persistOrder("customer-2", "PENDING");
        OrderItem item = persistItem(order.getId(), PRODUCT_ID + 1, VARIANT_ID + 1, 3, "149.99",
                OrderItem.OrderItemStatus.PENDING);

        // Given: an inventory event WITHOUT an eventId
        InventoryEvent event = InventoryEvent.reserved(VARIANT_ID + 1, PRODUCT_ID + 1, 3, 0);
        event.setEventId(null);

        // When: the same event is delivered twice
        inventoryEventListener.handleInventoryEvent(event);
        inventoryEventListener.handleInventoryEvent(event);

        // Then: nothing is recorded as processed, but the item is still reserved
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(processedEventRepository.count().block())
                    .as("events without an eventId are not tracked")
                    .isEqualTo(0);

            OrderItem updatedItem = orderItemRepository.findById(item.getId()).block();
            assertThat(updatedItem).isNotNull();
            assertThat(updatedItem.getStatus()).isEqualTo(OrderItem.OrderItemStatus.RESERVED);
        });
    }

    private Order persistOrder(String customerId, String status) {
        Order order = new Order();
        order.setCustomerId(customerId);
        order.setCustomerEmail(customerId + "@example.com");
        order.setStatus(status);
        order.setTotalAmount(BigDecimal.ZERO);
        Order saved = orderRepository.save(order).block();
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        return saved;
    }

    private OrderItem persistItem(Long orderId, Long productId, Long variantId, int quantity,
                                  String unitPrice, OrderItem.OrderItemStatus status) {
        OrderItem item = new OrderItem(null, orderId, productId, variantId, "TEST-001",
                "Test Product", quantity, 0, null, new BigDecimal(unitPrice), status);
        OrderItem saved = orderItemRepository.save(item).block();
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        return saved;
    }
}
