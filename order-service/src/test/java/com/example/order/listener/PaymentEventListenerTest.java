package com.example.order.listener;

import com.example.common.event.BaseEvent;
import com.example.common.event.OrderEvent;
import com.example.common.event.PaymentEvent;
import com.example.common.event.ReactiveIdempotentEventProcessor;
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

import reactor.core.publisher.Mono;

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
    private ReactiveIdempotentEventProcessor idempotentEventProcessor;

    @Mock
    private OrderService orderService;

    private PaymentEventListener listener;

    private Order order;
    private OrderItem item1;
    private OrderItem item2;

    @BeforeEach
    void setUp() {
        listener = new PaymentEventListener(idempotentEventProcessor, orderService);

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
            java.util.function.Function<PaymentEvent, reactor.core.publisher.Mono<Void>> handler = invocation.getArgument(1);
            return handler.apply(event);
        }).when(idempotentEventProcessor).process(any(), any());
    }

    @Test
    void handlePaymentCaptured_shouldTransitionOrderToConfirmedAndItemsToReserved() {
        PaymentEvent event = createPaymentEvent("CAPTURED", PaymentEvent.PaymentStatus.CAPTURED);

        when(orderService.handlePaymentCaptured(1L, event.getAmount())).thenReturn(Mono.empty());

        mockIdempotentProcessor(event);

        listener.handlePaymentEvent(event);

        verify(orderService).handlePaymentCaptured(1L, event.getAmount());
    }

    @Test
    void handlePaymentFailed_shouldCallOrderServiceHandlePaymentFailed() {
        PaymentEvent event = createPaymentEvent("FAILED", PaymentEvent.PaymentStatus.FAILED);

        when(orderService.handlePaymentFailed(1L)).thenReturn(Mono.empty());

        mockIdempotentProcessor(event);

        listener.handlePaymentEvent(event);

        verify(orderService).handlePaymentFailed(1L);
    }

    @Test
    void handlePaymentRefunded_shouldCallOrderServiceHandlePaymentRefunded() {
        PaymentEvent event = createPaymentEvent("REFUNDED", PaymentEvent.PaymentStatus.REFUNDED);

        when(orderService.handlePaymentRefunded(1L)).thenReturn(Mono.empty());

        mockIdempotentProcessor(event);

        listener.handlePaymentEvent(event);

        verify(orderService).handlePaymentRefunded(1L);
    }

    @Test
    void handlePartiallyRefunded_shouldCallOrderServiceHandlePaymentPartiallyRefunded() {
        PaymentEvent event = createPaymentEvent("REFUNDED", PaymentEvent.PaymentStatus.PARTIALLY_REFUNDED);

        when(orderService.handlePaymentPartiallyRefunded(1L)).thenReturn(Mono.empty());

        mockIdempotentProcessor(event);

        listener.handlePaymentEvent(event);

        verify(orderService).handlePaymentPartiallyRefunded(1L);
    }

    @Test
    void handlePaymentAuthorized_shouldCallOrderServiceHandlePaymentAuthorized() {
        PaymentEvent event = createPaymentEvent("AUTHORIZED", PaymentEvent.PaymentStatus.AUTHORIZED);

        when(orderService.handlePaymentAuthorized(1L)).thenReturn(Mono.empty());

        mockIdempotentProcessor(event);

        listener.handlePaymentEvent(event);

        verify(orderService).handlePaymentAuthorized(1L);
    }

    @Test
    void handlePaymentEvent_shouldSkipDuplicateEvent() {
        PaymentEvent event = createPaymentEvent("CAPTURED", PaymentEvent.PaymentStatus.CAPTURED);

        doAnswer(invocation -> {
            return Mono.empty();
        }).when(idempotentEventProcessor).process(any(), any());

        listener.handlePaymentEvent(event);

        verify(orderService, never()).handlePaymentCaptured(anyLong(), any());
    }

    @Test
    void handlePaymentEvent_shouldProcessEventWithoutIdempotency_whenEventIdMissing() {
        PaymentEvent event = createPaymentEvent("CAPTURED", PaymentEvent.PaymentStatus.CAPTURED);
        ReflectionTestUtils.setField(event, "eventId", null);

        when(orderService.handlePaymentCaptured(1L, event.getAmount())).thenReturn(Mono.empty());

        mockIdempotentProcessor(event);

        listener.handlePaymentEvent(event);

        verify(orderService).handlePaymentCaptured(1L, event.getAmount());
    }
}
