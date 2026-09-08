package com.example.order.listener;

import com.example.common.event.BaseEvent;
import com.example.common.event.IdempotentEventProcessor;
import com.example.common.event.OrderEvent;
import com.example.common.event.PaymentEvent;
import com.example.common.exception.ResourceNotFoundException;
import com.example.order.model.Order;
import com.example.order.model.OrderItem;
import com.example.order.repository.OrderItemRepository;
import com.example.order.repository.OrderRepository;
import com.example.order.service.OrderService;
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
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentEventListenerTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private IdempotentEventProcessor idempotentEventProcessor;

    @Mock
    private OrderService orderService;

    private PaymentEventListener listener;

    private Order order;
    private OrderItem item1;
    private OrderItem item2;

    @BeforeEach
    void setUp() {
        listener = new PaymentEventListener(orderRepository, orderItemRepository, idempotentEventProcessor, orderService);

        order = new Order();
        ReflectionTestUtils.setField(order, "id", 1L);
        ReflectionTestUtils.setField(order, "customerId", "CUST-001");
        ReflectionTestUtils.setField(order, "status", "PENDING");
        ReflectionTestUtils.setField(order, "totalAmount", new BigDecimal("1999.98"));

        item1 = new OrderItem();
        ReflectionTestUtils.setField(item1, "id", 1L);
        ReflectionTestUtils.setField(item1, "order", order);
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
        ReflectionTestUtils.setField(item2, "order", order);
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

    private PaymentEvent createPaymentEvent(String eventType, PaymentEvent.PaymentStatus status) {
        PaymentEvent event = new PaymentEvent();
        ReflectionTestUtils.setField(event, "eventType", eventType);
        ReflectionTestUtils.setField(event, "eventId", UUID.randomUUID());
        ReflectionTestUtils.setField(event, "orderId", 1L);
        ReflectionTestUtils.setField(event, "paymentId", 100L);
        ReflectionTestUtils.setField(event, "customerId", "CUST-001");
        ReflectionTestUtils.setField(event, "customerEmail", "customer@example.com");
        ReflectionTestUtils.setField(event, "amount", new BigDecimal("1999.98"));
        ReflectionTestUtils.setField(event, "currency", "USD");
        ReflectionTestUtils.setField(event, "status", status);
        ReflectionTestUtils.setField(event, "transactionId", "txn_123456");
        ReflectionTestUtils.setField(event, "timestamp", LocalDateTime.now());
        return event;
    }

    private void mockIdempotentProcessor(PaymentEvent event) {
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Consumer<PaymentEvent> handler = invocation.getArgument(1);
            handler.accept(event);
            return null;
        }).when(idempotentEventProcessor).process(any(BaseEvent.class), any(Consumer.class));
    }

    @Test
    void handlePaymentCaptured_shouldTransitionOrderToConfirmedAndItemsToReserved() {
        PaymentEvent event = createPaymentEvent("CAPTURED", PaymentEvent.PaymentStatus.CAPTURED);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(item1).thenReturn(item2);

        mockIdempotentProcessor(event);

        listener.handlePaymentEvent(event);

        verify(orderRepository).findById(1L);
        verify(orderRepository).save(argThat(o -> "CONFIRMED".equals(o.getStatus())));
        verify(orderItemRepository, times(2)).save(any(OrderItem.class));
        verify(orderService).publishOrderEvent(any(OrderEvent.class));
    }

    @Test
    void handlePaymentCaptured_shouldSkip_whenOrderNotPending() {
        PaymentEvent event = createPaymentEvent("CAPTURED", PaymentEvent.PaymentStatus.CAPTURED);
        ReflectionTestUtils.setField(order, "status", "CONFIRMED");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        mockIdempotentProcessor(event);

        listener.handlePaymentEvent(event);

        verify(orderRepository).findById(1L);
        verify(orderRepository, never()).save(any());
        verify(orderItemRepository, never()).save(any());
        verify(orderService, never()).publishOrderEvent(any());
    }

    @Test
    void handlePaymentFailed_shouldTransitionOrderToCancelledAndItemsToCancelled() {
        PaymentEvent event = createPaymentEvent("FAILED", PaymentEvent.PaymentStatus.FAILED);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(item1).thenReturn(item2);

        mockIdempotentProcessor(event);

        listener.handlePaymentEvent(event);

        verify(orderRepository).findById(1L);
        verify(orderRepository).save(argThat(o -> "CANCELLED".equals(o.getStatus())));
        verify(orderItemRepository, times(2)).save(argThat(i -> i.getStatus() == OrderItem.OrderItemStatus.CANCELLED));
        verify(orderService).publishOrderEvent(any(OrderEvent.class));
    }

    @Test
    void handlePaymentFailed_shouldSkip_whenOrderNotPending() {
        PaymentEvent event = createPaymentEvent("FAILED", PaymentEvent.PaymentStatus.FAILED);
        ReflectionTestUtils.setField(order, "status", "CONFIRMED");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        mockIdempotentProcessor(event);

        listener.handlePaymentEvent(event);

        verify(orderRepository).findById(1L);
        verify(orderRepository, never()).save(any());
        verify(orderItemRepository, never()).save(any());
        verify(orderService, never()).publishOrderEvent(any());
    }

    @Test
    void handlePaymentRefunded_shouldCancelItemsAndSetOrderToCancelled_whenAllItemsRefunded() {
        ReflectionTestUtils.setField(order, "status", "CONFIRMED");
        ReflectionTestUtils.setField(item1, "status", OrderItem.OrderItemStatus.RESERVED);
        ReflectionTestUtils.setField(item2, "status", OrderItem.OrderItemStatus.RESERVED);

        PaymentEvent event = createPaymentEvent("REFUNDED", PaymentEvent.PaymentStatus.REFUNDED);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(item1).thenReturn(item2);

        mockIdempotentProcessor(event);

        listener.handlePaymentEvent(event);

        verify(orderRepository).save(argThat(o -> "CANCELLED".equals(o.getStatus())));
        verify(orderItemRepository, times(2)).save(argThat(i -> i.getStatus() == OrderItem.OrderItemStatus.CANCELLED));
        verify(orderService).publishOrderEvent(any(OrderEvent.class));
    }

    @Test
    void handlePartiallyRefunded_shouldCancelReservedItems() {
        ReflectionTestUtils.setField(order, "status", "CONFIRMED");
        ReflectionTestUtils.setField(item1, "status", OrderItem.OrderItemStatus.RESERVED);
        ReflectionTestUtils.setField(item2, "status", OrderItem.OrderItemStatus.SHIPPED);

        PaymentEvent event = createPaymentEvent("REFUNDED", PaymentEvent.PaymentStatus.PARTIALLY_REFUNDED);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(item1);

        mockIdempotentProcessor(event);

        listener.handlePaymentEvent(event);

        verify(orderItemRepository).save(argThat(i -> i.getStatus() == OrderItem.OrderItemStatus.CANCELLED && i.getId() == 1L));
        verify(orderItemRepository, never()).save(argThat(i -> i.getId() == 2L));
        verify(orderService).publishOrderEvent(any(OrderEvent.class));
    }

    @Test
    void handlePaymentEvent_shouldSkipDuplicateEvent() {
        PaymentEvent event = createPaymentEvent("CAPTURED", PaymentEvent.PaymentStatus.CAPTURED);

        doAnswer(invocation -> {
            // Don't call handler - simulates duplicate detection
            return null;
        }).when(idempotentEventProcessor).process(any(BaseEvent.class), any(Consumer.class));

        listener.handlePaymentEvent(event);

        verify(orderRepository, never()).findById(anyLong());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void handlePaymentEvent_shouldProcessEventWithoutIdempotency_whenEventIdMissing() {
        PaymentEvent event = createPaymentEvent("CAPTURED", PaymentEvent.PaymentStatus.CAPTURED);
        ReflectionTestUtils.setField(event, "eventId", null);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(item1).thenReturn(item2);

        mockIdempotentProcessor(event);

        listener.handlePaymentEvent(event);

        verify(orderRepository).findById(1L);
        verify(orderRepository).save(argThat(o -> "CONFIRMED".equals(o.getStatus())));
    }
}