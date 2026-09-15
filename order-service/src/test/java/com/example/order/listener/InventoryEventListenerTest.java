package com.example.order.listener;

import com.example.common.event.BaseEvent;
import com.example.common.event.InventoryEvent;
import com.example.common.event.OrderEvent;
import com.example.common.event.ReactiveIdempotentEventProcessor;
import com.example.common.exception.ResourceNotFoundException;
import com.example.order.model.Order;
import com.example.order.model.OrderItem;
import com.example.order.outbox.R2dbcOutboxEventPublisher;
import com.example.order.repository.OrderItemRepository;
import com.example.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryEventListenerTest {

    @Mock(lenient = true)
    private OrderRepository orderRepository;

    @Mock(lenient = true)
    private OrderItemRepository orderItemRepository;

    @Mock
    private ReactiveIdempotentEventProcessor idempotentEventProcessor;

    @Mock(lenient = true)
    private R2dbcOutboxEventPublisher outboxPublisher;

    private InventoryEventListener listener;

    private Order order;
    private OrderItem item1;
    private OrderItem item2;

    @BeforeEach
    void setUp() {
        listener = new InventoryEventListener(orderRepository, orderItemRepository, idempotentEventProcessor, outboxPublisher);

        order = new Order();
        ReflectionTestUtils.setField(order, "id", 1L);
        ReflectionTestUtils.setField(order, "customerId", "CUST-001");
        ReflectionTestUtils.setField(order, "status", "PENDING");
        ReflectionTestUtils.setField(order, "totalAmount", new BigDecimal("1999.98"));

        item1 = new OrderItem();
        ReflectionTestUtils.setField(item1, "id", 1L);
        ReflectionTestUtils.setField(item1, "orderId", 1L);
        ReflectionTestUtils.setField(item1, "productId", 1L);
        ReflectionTestUtils.setField(item1, "variantId", 1L);
        ReflectionTestUtils.setField(item1, "skuCode", "LAPTOP-13-SILVER");
        ReflectionTestUtils.setField(item1, "productName", "Laptop 13 Silver");
        ReflectionTestUtils.setField(item1, "quantityOrdered", 2);
        ReflectionTestUtils.setField(item1, "quantityShipped", 0);
        ReflectionTestUtils.setField(item1, "unitPrice", new BigDecimal("999.99"));
        ReflectionTestUtils.setField(item1, "status", OrderItem.OrderItemStatus.PENDING);
        ReflectionTestUtils.setField(item1, "reservedAt", LocalDateTime.now());

        item2 = new OrderItem();
        ReflectionTestUtils.setField(item2, "id", 2L);
        ReflectionTestUtils.setField(item2, "orderId", 1L);
        ReflectionTestUtils.setField(item2, "productId", 2L);
        ReflectionTestUtils.setField(item2, "variantId", 2L);
        ReflectionTestUtils.setField(item2, "skuCode", "MOUSE-WIRELESS");
        ReflectionTestUtils.setField(item2, "productName", "Wireless Mouse");
        ReflectionTestUtils.setField(item2, "quantityOrdered", 1);
        ReflectionTestUtils.setField(item2, "quantityShipped", 0);
        ReflectionTestUtils.setField(item2, "unitPrice", new BigDecimal("49.99"));
        ReflectionTestUtils.setField(item2, "status", OrderItem.OrderItemStatus.PENDING);
        ReflectionTestUtils.setField(item2, "reservedAt", LocalDateTime.now());

        order.getItems().add(item1);
        order.getItems().add(item2);
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
    void handleStockReserved_shouldSetItemToReservedAndOrderToReserved_whenFullyReserved() {
        InventoryEvent event = createInventoryEvent("RESERVED", 1L, 2, 0);

        // Set item2 to RESERVED so all items are reserved
        ReflectionTestUtils.setField(item2, "status", OrderItem.OrderItemStatus.RESERVED);

        when(orderItemRepository.findByVariantIdAndStatus(1L, OrderItem.OrderItemStatus.PENDING))
                .thenReturn(Mono.just(item1));
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(Mono.just(item1));
        when(orderRepository.findById(1L)).thenReturn(Mono.just(order));
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(order));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(Flux.just(item1, item2));
        when(outboxPublisher.saveEvent(anyString(), anyString(), anyString(), any())).thenReturn(Mono.empty());

        mockIdempotentProcessor(event);

        listener.handleInventoryEvent(event);

        // Allow async operations to complete
        try { Thread.sleep(100); } catch (InterruptedException e) {}

        verify(orderItemRepository).findByVariantIdAndStatus(1L, OrderItem.OrderItemStatus.PENDING);
        verify(orderItemRepository).save(argThat(i -> i.getStatus() == OrderItem.OrderItemStatus.RESERVED));
        verify(orderRepository).save(argThat(o -> "RESERVED".equals(o.getStatus())));
        verify(outboxPublisher).saveEvent(eq("Order"), eq("1"), eq("ORDER_UPDATED"), any(OrderEvent.class));
    }

    @Test
    void handleStockReserved_shouldSetItemToBackordered_whenPartiallyReserved() {
        InventoryEvent event = createInventoryEvent("RESERVED", 1L, 1, 1);

        when(orderItemRepository.findByVariantIdAndStatus(1L, OrderItem.OrderItemStatus.PENDING))
                .thenReturn(Mono.just(item1));
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(Mono.just(item1));
        when(orderRepository.findById(1L)).thenReturn(Mono.just(order));
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(order));

        mockIdempotentProcessor(event);

        listener.handleInventoryEvent(event);

        // Allow async operations to complete
        try { Thread.sleep(100); } catch (InterruptedException e) {}

        verify(orderItemRepository).save(argThat(i -> i.getStatus() == OrderItem.OrderItemStatus.BACKORDERED));
    }

    @Test
    void handleStockReserved_shouldCancelOrder_whenFullyBackordered() {
        InventoryEvent event = createInventoryEvent("RESERVED", 1L, 0, 2);

        // Set item2 to BACKORDERED so all items are CANCELLED or BACKORDERED
        ReflectionTestUtils.setField(item2, "status", OrderItem.OrderItemStatus.BACKORDERED);

        when(orderItemRepository.findByVariantIdAndStatus(1L, OrderItem.OrderItemStatus.PENDING))
                .thenReturn(Mono.just(item1));
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(Mono.just(item1));
        when(orderRepository.findById(1L)).thenReturn(Mono.just(order));
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(order));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(Flux.just(item1, item2));
        when(outboxPublisher.saveEvent(anyString(), anyString(), anyString(), any())).thenReturn(Mono.empty());

        mockIdempotentProcessor(event);

        listener.handleInventoryEvent(event);

        // Allow async operations to complete
        try { Thread.sleep(100); } catch (InterruptedException e) {}

        ArgumentCaptor<OrderItem> itemCaptor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemRepository).save(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getStatus()).isEqualTo(OrderItem.OrderItemStatus.CANCELLED);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo("CANCELLED");

        verify(outboxPublisher).saveEvent(eq("Order"), eq("1"), eq("ORDER_CANCELLED"), any(OrderEvent.class));
    }

    @Test
    void handleStockReserved_shouldSkip_whenItemNotFound() {
        InventoryEvent event = createInventoryEvent("RESERVED", 999L, 2, 0);

        when(orderItemRepository.findByVariantIdAndStatus(999L, OrderItem.OrderItemStatus.PENDING))
                .thenReturn(Mono.empty());

        mockIdempotentProcessor(event);

        listener.handleInventoryEvent(event);

        verify(orderItemRepository).findByVariantIdAndStatus(999L, OrderItem.OrderItemStatus.PENDING);
        verify(orderItemRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void handleStockReserved_shouldNotTransitionOrder_whenNotAllItemsReserved() {
        // item2 stays PENDING
        InventoryEvent event = createInventoryEvent("RESERVED", 1L, 2, 0);

        when(orderItemRepository.findByVariantIdAndStatus(1L, OrderItem.OrderItemStatus.PENDING))
                .thenReturn(Mono.just(item1));
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(Mono.just(item1));
        when(orderRepository.findById(1L)).thenReturn(Mono.just(order));
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(order));

        mockIdempotentProcessor(event);

        listener.handleInventoryEvent(event);

        verify(orderItemRepository).save(argThat(i -> i.getStatus() == OrderItem.OrderItemStatus.RESERVED));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void handleInventoryEvent_shouldSkipDuplicateEvent() {
        InventoryEvent event = createInventoryEvent("RESERVED", 1L, 2, 0);

        doAnswer(invocation -> {
            // Don't call handler - simulates duplicate detection
            return Mono.empty();
        }).when(idempotentEventProcessor).process(any(BaseEvent.class), any(Function.class));

        listener.handleInventoryEvent(event);

        verify(orderRepository, never()).findById(anyLong());
        verify(orderRepository, never()).save(any());
    }
}