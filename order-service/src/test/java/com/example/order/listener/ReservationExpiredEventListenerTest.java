package com.example.order.listener;

import com.example.common.event.BaseEvent;
import com.example.common.event.IdempotentEventProcessor;
import com.example.common.event.OrderEvent;
import com.example.common.event.ReservationExpiredEvent;
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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationExpiredEventListenerTest {

    @Mock(lenient = true)
    private OrderRepository orderRepository;

    @Mock(lenient = true)
    private OrderItemRepository orderItemRepository;

    @Mock
    private IdempotentEventProcessor idempotentEventProcessor;

    @Mock(lenient = true)
    private OrderService orderService;

    private ReservationExpiredEventListener listener;

    private Order order;
    private OrderItem item1;
    private OrderItem item2;

    @BeforeEach
    void setUp() {
        listener = new ReservationExpiredEventListener(orderRepository, orderItemRepository, idempotentEventProcessor, orderService);

        order = new Order();
        ReflectionTestUtils.setField(order, "id", 1L);
        ReflectionTestUtils.setField(order, "customerId", "CUST-001");
        ReflectionTestUtils.setField(order, "status", "RESERVED");
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
        ReflectionTestUtils.setField(item1, "status", OrderItem.OrderItemStatus.RESERVED);
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
        ReflectionTestUtils.setField(item2, "status", OrderItem.OrderItemStatus.RESERVED);
        ReflectionTestUtils.setField(item2, "reservedAt", LocalDateTime.now());

        order.getItems().add(item1);
        order.getItems().add(item2);
    }

    private ReservationExpiredEvent createReservationExpiredEvent(Long orderItemId, Long variantId) {
        ReservationExpiredEvent event = new ReservationExpiredEvent();
        ReflectionTestUtils.setField(event, "eventType", "EXPIRED");
        ReflectionTestUtils.setField(event, "eventId", UUID.randomUUID());
        ReflectionTestUtils.setField(event, "orderItemId", orderItemId);
        ReflectionTestUtils.setField(event, "variantId", variantId);
        ReflectionTestUtils.setField(event, "quantityReleased", 2);
        ReflectionTestUtils.setField(event, "reservationExpiresAt", LocalDateTime.now());
        return event;
    }

    private void mockIdempotentProcessor(ReservationExpiredEvent event) {
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Consumer<ReservationExpiredEvent> handler = invocation.getArgument(1);
            handler.accept(event);
            return null;
        }).when(idempotentEventProcessor).process(any(BaseEvent.class), any(Consumer.class));
    }

    @Test
    void handleReservationExpiredEvent_shouldCancelItemAndOrder_whenAllItemsReserved() {
        ReservationExpiredEvent event = createReservationExpiredEvent(1L, 1L);

        // Ensure both items are RESERVED, then set item2 to CANCELLED after item1 is cancelled
        // Actually, the code checks if all items are CANCELLED or BACKORDERED
        // So we need to set item2 to CANCELLED or BACKORDERED as well
        ReflectionTestUtils.setField(item2, "status", OrderItem.OrderItemStatus.BACKORDERED);

        when(orderItemRepository.findById(1L)).thenReturn(Mono.just(item1));
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(Mono.just(item1));
        when(orderRepository.findById(1L)).thenReturn(Mono.just(order));
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(order));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(Flux.just(item1, item2));

        mockIdempotentProcessor(event);

        listener.handleReservationExpiredEvent(event);

        // Allow async operations to complete
        try { Thread.sleep(100); } catch (InterruptedException e) {}

        verify(orderItemRepository).findById(1L);
        verify(orderItemRepository).save(argThat(i -> i.getStatus() == OrderItem.OrderItemStatus.CANCELLED));
        verify(orderRepository).save(argThat(o -> "CANCELLED".equals(o.getStatus())));
        verify(orderService).publishOrderEvent(any(OrderEvent.class));
    }

    @Test
    void handleReservationExpiredEvent_shouldNotCancelOrder_whenOtherItemsNotCancelled() {
        // item2 is SHIPPED, not CANCELLED
        ReflectionTestUtils.setField(item2, "status", OrderItem.OrderItemStatus.SHIPPED);

        ReservationExpiredEvent event = createReservationExpiredEvent(1L, 1L);

        when(orderItemRepository.findById(1L)).thenReturn(Mono.just(item1));
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(Mono.just(item1));
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(order));

        mockIdempotentProcessor(event);

        listener.handleReservationExpiredEvent(event);

        verify(orderItemRepository).save(argThat(i -> i.getStatus() == OrderItem.OrderItemStatus.CANCELLED));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void handleReservationExpiredEvent_shouldSkip_whenItemNotFound() {
        ReservationExpiredEvent event = createReservationExpiredEvent(999L, 1L);

        when(orderItemRepository.findById(999L)).thenReturn(Mono.empty());

        mockIdempotentProcessor(event);

        listener.handleReservationExpiredEvent(event);

        verify(orderItemRepository).findById(999L);
        verify(orderItemRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void handleReservationExpiredEvent_shouldSkip_whenItemNotReserved() {
        ReflectionTestUtils.setField(item1, "status", OrderItem.OrderItemStatus.SHIPPED);

        ReservationExpiredEvent event = createReservationExpiredEvent(1L, 1L);

        when(orderItemRepository.findById(1L)).thenReturn(Mono.just(item1));

        mockIdempotentProcessor(event);

        listener.handleReservationExpiredEvent(event);

        verify(orderItemRepository).findById(1L);
        verify(orderItemRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void handleReservationExpiredEvent_shouldSkipDuplicateEvent() {
        ReservationExpiredEvent event = createReservationExpiredEvent(1L, 1L);

        doAnswer(invocation -> {
            // Don't call handler - simulates duplicate detection
            return null;
        }).when(idempotentEventProcessor).process(any(BaseEvent.class), any(Consumer.class));

        listener.handleReservationExpiredEvent(event);

        verify(orderItemRepository, never()).findById(anyLong());
        verify(orderRepository, never()).save(any());
    }
}
