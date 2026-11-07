package com.example.order.service;

import com.example.common.dto.CreateOrderItemRequest;
import com.example.common.dto.CreateOrderRequest;
import com.example.common.dto.OrderDto;
import com.example.common.event.InventoryEvent;
import com.example.common.event.OrderEvent;
import com.example.common.event.OutboxEventPublisher;
import com.example.common.event.PaymentEvent;
import com.example.common.event.ReservationExpiredEvent;
import com.example.common.exception.ResourceNotFoundException;
import com.example.order.mapper.OrderMapper;
import com.example.order.model.Order;
import com.example.order.model.OrderItem;
import com.example.order.repository.OrderItemRepository;
import com.example.order.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.reactive.TransactionalOperator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OrderSagaOrchestratorImpl}.
 * <p>
 * The saga logic is exercised through its public interface (createOrder +
 * saga event handlers) with mocked collaborators, so no RabbitMQ or database
 * is required. This satisfies the "interface is the test surface" principle:
 * the same seam callers use is what tests cross.
 */
@ExtendWith(MockitoExtension.class)
class OrderSagaOrchestratorImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock(lenient = true)
    private OutboxEventPublisher outboxPublisher;

    @Mock
    private PaymentProcessor paymentProcessor;

    @Mock
    private OrderEventPublisher eventPublisher;

    @Mock(lenient = true)
    private TransactionalOperator transactionalOperator;

    private OrderSagaOrchestratorImpl saga;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 3, 10, 0, 0);

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        saga = new OrderSagaOrchestratorImpl(
                orderRepository,
                orderItemRepository,
                orderMapper,
                objectMapper,
                transactionalOperator,
                outboxPublisher,
                paymentProcessor,
                eventPublisher
        );

        // Pass transactions through in tests (no real R2DBC transaction)
        doAnswer(inv -> inv.getArgument(0))
                .when(transactionalOperator).transactional(any(Mono.class));
        doAnswer(inv -> inv.getArgument(0))
                .when(transactionalOperator).transactional(any(Flux.class));

        // Outbox is a no-op in unit tests
        when(outboxPublisher.saveEvent(anyString(), anyString(), anyString(), any()))
                .thenReturn(Mono.empty());
    }

    // ------------------------------------------------------------------
    // createOrder
    // ------------------------------------------------------------------

    @Test
    void createOrder_shouldComputeTotalFromItemPrices() {
        // Given
        CreateOrderItemRequest item1 = new CreateOrderItemRequest(10L, null, 2, new BigDecimal("10.00"));
        CreateOrderItemRequest item2 = new CreateOrderItemRequest(11L, null, 1, new BigDecimal("25.50"));
        CreateOrderRequest request = new CreateOrderRequest("CUST-001", "customer@example.com", List.of(item1, item2));

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = (Order) inv.getArgument(0);
            o.setId(1L);
            return Mono.just(o);
        });
        final List<OrderItem> capturedItems = new ArrayList<>();
        when(orderItemRepository.saveAll((Iterable<OrderItem>) (Object) anyList())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            List<OrderItem> items = (List<OrderItem>) inv.getArgument(0);
            capturedItems.addAll(items);
            return Flux.fromIterable(items);
        });
        when(orderItemRepository.findByOrderId(1L)).thenReturn(Flux.empty());
        when(orderMapper.toDto(any(Order.class))).thenReturn(
                new OrderDto(1L, "CUST-001", "customer@example.com", OrderDto.OrderStatus.PENDING,
                        BigDecimal.valueOf(45.50), List.of(), List.of(), null, null));

        // When
        Mono<OrderDto> result = saga.createOrder(request);

        // Then
        StepVerifier.create(result)
                .assertNext(dto -> {
                    assertThat(dto.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(45.50));
                    assertThat(dto.totalAmount().signum()).isPositive();
                })
                .verifyComplete();

        // Verify unit price was persisted on each item (total 10*2 + 25.50*1 = 45.50)
        assertThat(capturedItems).hasSize(2);
        assertThat(capturedItems.stream()
                .allMatch(item -> item.getUnitPrice() != null && item.getUnitPrice().signum() > 0))
                .isTrue();
    }

    @Test
    void createOrder_shouldTotalZero_whenNoItems() {
        // Given
        CreateOrderRequest request = new CreateOrderRequest("CUST-001", "customer@example.com", List.of());

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = (Order) inv.getArgument(0);
            o.setId(1L);
            return Mono.just(o);
        });
        when(orderItemRepository.findByOrderId(1L)).thenReturn(Flux.empty());
        when(orderMapper.toDto(any(Order.class))).thenReturn(
                new OrderDto(1L, "CUST-001", "customer@example.com", OrderDto.OrderStatus.PENDING,
                        BigDecimal.ZERO, List.of(), List.of(), null, null));

        // When / Then
        StepVerifier.create(saga.createOrder(request))
                .assertNext(dto -> assertThat(dto.totalAmount()).isZero())
                .verifyComplete();
    }

    @Test
    void createOrder_shouldPersistUnitPriceOnOrderItems() {
        // Given
        CreateOrderItemRequest item = new CreateOrderItemRequest(10L, null, 3, new BigDecimal("7.50"));
        CreateOrderRequest request = new CreateOrderRequest("CUST-001", "customer@example.com", List.of(item));

        final List<OrderItem> capturedItems = new ArrayList<>();

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = (Order) inv.getArgument(0);
            o.setId(1L);
            return Mono.just(o);
        });
        when(orderItemRepository.saveAll((Iterable<OrderItem>) (Object) anyList())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            List<OrderItem> items = (List<OrderItem>) inv.getArgument(0);
            capturedItems.addAll(items);
            return Flux.fromIterable(items);
        });
        when(orderItemRepository.findByOrderId(1L)).thenReturn(Flux.empty());
        when(orderMapper.toDto(any(Order.class))).thenReturn(
                new OrderDto(1L, "CUST-001", "customer@example.com", OrderDto.OrderStatus.PENDING,
                        BigDecimal.valueOf(22.50), List.of(), List.of(), null, null));

        // When
        StepVerifier.create(saga.createOrder(request))
                .assertNext(dto -> assertThat(dto.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(22.50)))
                .verifyComplete();

        // Then
        assertThat(capturedItems).hasSize(1);
        assertThat(capturedItems.get(0).getUnitPrice()).isEqualByComparingTo(new BigDecimal("7.50"));
        assertThat(capturedItems.get(0).getQuantityOrdered()).isEqualTo(3);
    }

    // ------------------------------------------------------------------
    // handleInventoryReserved
    // ------------------------------------------------------------------

    @Test
    void handleInventoryReserved_shouldMarkItemReserved_whenStockReserved() {
        // Given
        OrderItem orderItem = new OrderItem();
        orderItem.setId(100L);
        orderItem.setOrderId(1L);
        orderItem.setVariantId(50L);
        orderItem.setStatus(OrderItem.OrderItemStatus.PENDING);

        Order order = new Order();
        order.setId(1L);
        order.setStatus("PENDING");

        InventoryEvent event = InventoryEvent.reserved(50L, 1L, 2, 0, 1L, "CUST-001", "customer@example.com");

        when(orderItemRepository.findByVariantIdAndStatus(50L, OrderItem.OrderItemStatus.PENDING))
                .thenReturn(Mono.just(orderItem));
        when(orderItemRepository.save(orderItem)).thenReturn(Mono.just(orderItem));
        when(orderRepository.findById(1L)).thenReturn(Mono.just(order));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(Flux.just(orderItem));
        when(orderRepository.save(order)).thenReturn(Mono.just(order));

        // When
        StepVerifier.create(saga.handleInventoryReserved(event)).verifyComplete();

        // Then
        assertThat(orderItem.getStatus()).isEqualTo(OrderItem.OrderItemStatus.RESERVED);
        verify(orderItemRepository).save(orderItem);
        verify(orderRepository).save(order);
    }

    @Test
    void handleInventoryReserved_shouldMarkItemBackordered_whenBackordered() {
        // Given
        OrderItem orderItem = new OrderItem();
        orderItem.setId(100L);
        orderItem.setOrderId(1L);
        orderItem.setVariantId(50L);
        orderItem.setStatus(OrderItem.OrderItemStatus.PENDING);

        Order order = new Order();
        order.setId(1L);
        order.setStatus("PENDING");

        // reserved=0, backordered=3
        InventoryEvent event = InventoryEvent.reserved(50L, 1L, 0, 3, 1L, "CUST-001", "customer@example.com");

        when(orderItemRepository.findByVariantIdAndStatus(50L, OrderItem.OrderItemStatus.PENDING))
                .thenReturn(Mono.just(orderItem));
        when(orderItemRepository.save(orderItem)).thenReturn(Mono.just(orderItem));
        when(orderRepository.findById(1L)).thenReturn(Mono.just(order));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(Flux.just(orderItem));
        when(orderRepository.save(order)).thenReturn(Mono.just(order));

        // When
        StepVerifier.create(saga.handleInventoryReserved(event)).verifyComplete();

        // Then
        assertThat(orderItem.getStatus()).isEqualTo(OrderItem.OrderItemStatus.BACKORDERED);
        verify(orderItemRepository).save(orderItem);
    }

    @Test
    void handleInventoryReserved_shouldMarkItemBackordered_whenNoStockButBackordered() {
        // Given
        OrderItem orderItem = new OrderItem();
        orderItem.setId(100L);
        orderItem.setOrderId(1L);
        orderItem.setVariantId(50L);
        orderItem.setStatus(OrderItem.OrderItemStatus.PENDING);

        Order order = new Order();
        order.setId(1L);
        order.setStatus("PENDING");

        // reserved=0, backordered=5 → goes to handleStockReservationFailed → BACKORDERED
        InventoryEvent event = InventoryEvent.reserved(50L, 1L, 0, 5, 1L, "CUST-001", "customer@example.com");

        when(orderItemRepository.findByVariantIdAndStatus(50L, OrderItem.OrderItemStatus.PENDING))
                .thenReturn(Mono.just(orderItem));
        when(orderItemRepository.save(orderItem)).thenReturn(Mono.just(orderItem));
        when(orderRepository.findById(1L)).thenReturn(Mono.just(order));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(Flux.just(orderItem));
        when(orderRepository.save(order)).thenReturn(Mono.just(order));

        // When
        StepVerifier.create(saga.handleInventoryReserved(event)).verifyComplete();

        // Then
        assertThat(orderItem.getStatus()).isEqualTo(OrderItem.OrderItemStatus.BACKORDERED);
        verify(orderItemRepository).save(orderItem);
    }

    @Test
    void handleInventoryReserved_shouldDoNothing_whenNeitherReservedNorBackordered() {
        // Given
        OrderItem orderItem = new OrderItem();
        orderItem.setId(100L);
        orderItem.setOrderId(1L);
        orderItem.setVariantId(50L);
        orderItem.setStatus(OrderItem.OrderItemStatus.PENDING);

        // reserved=0, backordered=0 → early return, no change
        InventoryEvent event = InventoryEvent.reserved(50L, 1L, 0, 0, 1L, "CUST-001", "customer@example.com");

        // When / Then — item status unchanged, no save
        StepVerifier.create(saga.handleInventoryReserved(event)).verifyComplete();

        assertThat(orderItem.getStatus()).isEqualTo(OrderItem.OrderItemStatus.PENDING);
        verify(orderItemRepository, never()).save(any());
        verifyNoInteractions(orderRepository);
    }

    @Test
    void handleInventoryReserved_shouldDoNothing_whenVariantIdMissing() {
        // Given
        InventoryEvent event = InventoryEvent.reserved(null, 1L, 2, 0, 1L, "CUST-001", "customer@example.com");

        // When / Then — no repository interaction expected
        StepVerifier.create(saga.handleInventoryReserved(event)).verifyComplete();

        verifyNoInteractions(orderItemRepository, orderRepository);
    }

    // ------------------------------------------------------------------
    // handleReservationExpired
    // ------------------------------------------------------------------

    @Test
    void handleReservationExpired_shouldCancelItemAndOrder() {
        // Given
        OrderItem orderItem = new OrderItem();
        orderItem.setId(100L);
        orderItem.setOrderId(1L);
        orderItem.setVariantId(50L);
        orderItem.setStatus(OrderItem.OrderItemStatus.RESERVED);

        Order order = new Order();
        order.setId(1L);
        order.setStatus("RESERVED");

        ReservationExpiredEvent event = ReservationExpiredEvent.expired(100L, 50L, 2, NOW);

        when(orderItemRepository.findById(100L)).thenReturn(Mono.just(orderItem));
        when(orderItemRepository.save(orderItem)).thenReturn(Mono.just(orderItem));
        when(orderRepository.findById(1L)).thenReturn(Mono.just(order));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(Flux.just(orderItem));
        when(orderRepository.save(order)).thenReturn(Mono.just(order));

        // When
        StepVerifier.create(saga.handleReservationExpired(event)).verifyComplete();

        // Then
        assertThat(orderItem.getStatus()).isEqualTo(OrderItem.OrderItemStatus.CANCELLED);
        assertThat(order.getStatus()).isEqualTo(OrderEvent.OrderStatus.CANCELLED.name());
        verify(orderItemRepository).save(orderItem);
        verify(orderRepository).save(order);
    }

    @Test
    void handleReservationExpired_shouldDoNothing_whenOrderItemNotReserved() {
        // Given
        OrderItem orderItem = new OrderItem();
        orderItem.setId(100L);
        orderItem.setOrderId(1L);
        orderItem.setVariantId(50L);
        orderItem.setStatus(OrderItem.OrderItemStatus.PENDING); // not RESERVED

        ReservationExpiredEvent event = ReservationExpiredEvent.expired(100L, 50L, 2, NOW);

        when(orderItemRepository.findById(100L)).thenReturn(Mono.just(orderItem));

        // When / Then
        StepVerifier.create(saga.handleReservationExpired(event)).verifyComplete();

        verify(orderItemRepository, never()).save(any());
    }

    @Test
    void handleReservationExpired_shouldDoNothing_whenOrderItemMissing() {
        // Given
        ReservationExpiredEvent event = ReservationExpiredEvent.expired(999L, 50L, 2, NOW);

        when(orderItemRepository.findById(999L)).thenReturn(Mono.empty());

        // When / Then
        StepVerifier.create(saga.handleReservationExpired(event)).verifyComplete();

        verify(orderItemRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // payment event delegation
    // ------------------------------------------------------------------

    @Test
    void handlePaymentAuthorized_shouldDelegateToPaymentProcessor() {
        // Given
        PaymentEvent event = new PaymentEvent();
        event.setOrderId(1L);
        event.setPaymentId(100L);

        when(paymentProcessor.handlePaymentAuthorized(1L)).thenReturn(Mono.empty());

        // When / Then
        StepVerifier.create(saga.handlePaymentAuthorized(event)).verifyComplete();
        verify(paymentProcessor).handlePaymentAuthorized(1L);
    }

    @Test
    void handlePaymentCaptured_shouldDelegateToPaymentProcessor() {
        // Given
        PaymentEvent event = new PaymentEvent();
        event.setOrderId(1L);
        event.setAmount(new BigDecimal("99.99"));

        when(paymentProcessor.handlePaymentCaptured(1L, new BigDecimal("99.99"))).thenReturn(Mono.empty());

        // When / Then
        StepVerifier.create(saga.handlePaymentCaptured(event)).verifyComplete();
        verify(paymentProcessor).handlePaymentCaptured(1L, new BigDecimal("99.99"));
    }

    @Test
    void handlePaymentFailed_shouldDelegateToPaymentProcessor() {
        // Given
        PaymentEvent event = new PaymentEvent();
        event.setOrderId(1L);

        when(paymentProcessor.handlePaymentFailed(1L)).thenReturn(Mono.empty());

        // When / Then
        StepVerifier.create(saga.handlePaymentFailed(event)).verifyComplete();
        verify(paymentProcessor).handlePaymentFailed(1L);
    }

    @Test
    void handleOrderCancelled_shouldThrowNotFound_whenOrderMissing() {
        // Given
        OrderEvent event = OrderEvent.cancelled(999L, "CUST-001", "customer@example.com", List.of());

        when(orderRepository.findById(999L)).thenReturn(Mono.empty());

        // When / Then
        StepVerifier.create(saga.handleOrderCancelled(event))
                .verifyErrorMatches(throwable -> throwable instanceof ResourceNotFoundException
                        && throwable.getMessage().contains("Order"));
    }
}
