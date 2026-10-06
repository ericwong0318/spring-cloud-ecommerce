package com.example.order.service;

import com.example.common.dto.CreateOrderRequest;
import com.example.common.dto.CreateOrderItemRequest;
import com.example.common.dto.OrderDto;
import com.example.common.dto.OrderItemDto;
import com.example.common.exception.ResourceNotFoundException;
import com.example.common.event.OrderEvent;
import com.example.order.mapper.OrderMapper;
import com.example.order.model.Order;
import com.example.order.model.OrderItem;
import com.example.common.event.OutboxEventPublisher;
import com.example.order.repository.OrderItemRepository;
import com.example.order.repository.OrderRepository;
import com.example.order.service.OrderEventPublisher;
import com.example.order.service.PaymentProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.reactive.TransactionalOperator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@TestPropertySource("classpath:application-test.yml")
class OrderServiceTest {

    @Mock(lenient = true)
    private OrderRepository orderRepository;

    @Mock(lenient = true)
    private OrderItemRepository orderItemRepository;

    @Mock(lenient = true)
    private OrderMapper orderMapper;

    @Mock(lenient = true)
    private OutboxEventPublisher outboxPublisher;

    @Mock(lenient = true)
    private OrderSagaOrchestratorImpl sagaOrchestrator;

    @Mock(lenient = true)
    private PaymentProcessor paymentProcessor;

    @Mock(lenient = true)
    private OrderEventPublisher eventPublisher;

    @Mock(lenient = true)
    private TransactionalOperator transactionalOperator;

    @Mock(lenient = true)
    private R2dbcTransactionManager r2dbcTransactionManager;

    private OrderService orderService;

    private OrderDto orderDto;
    private Order order;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                orderRepository,
                orderItemRepository,
                orderMapper,
                new ObjectMapper().registerModule(new JavaTimeModule()),
                transactionalOperator,
                sagaOrchestrator,
                paymentProcessor,
                eventPublisher
        );

        // Mock TransactionalOperator to pass through the publisher (no actual transaction in tests)
        doAnswer(inv -> inv.getArgument(0)).when(transactionalOperator).transactional(any(Mono.class));
        doAnswer(inv -> inv.getArgument(0)).when(transactionalOperator).transactional(any(Flux.class));

        // Mock outboxPublisher to return empty Mono
        when(outboxPublisher.saveEvent(anyString(), anyString(), anyString(), any())).thenReturn(Mono.empty());

        // Mock new dependencies - delegate to "real" behavior for createOrder tests
        when(sagaOrchestrator.createOrder(any(CreateOrderRequest.class))).thenAnswer(inv -> {
            CreateOrderRequest req = inv.getArgument(0);
            return Mono.just(new OrderDto(1L, req.customerId(), "customer@example.com", OrderDto.OrderStatus.CREATED, BigDecimal.ZERO, Collections.emptyList(), Collections.emptyList(), null, null));
        });
        when(sagaOrchestrator.handleReservationExpiry(anyLong())).thenReturn(Mono.empty());
        when(paymentProcessor.processRefund(anyLong())).thenReturn(Mono.empty());
        when(paymentProcessor.handlePaymentAuthorized(anyLong())).thenReturn(Mono.empty());
        when(paymentProcessor.handlePaymentCaptured(anyLong(), any(BigDecimal.class))).thenReturn(Mono.empty());
        when(paymentProcessor.handlePaymentFailed(anyLong())).thenReturn(Mono.empty());
        when(paymentProcessor.handlePaymentRefunded(anyLong())).thenReturn(Mono.empty());
        when(paymentProcessor.handlePaymentPartiallyRefunded(anyLong())).thenReturn(Mono.empty());
        when(eventPublisher.publishOrderCreated(any(Order.class))).thenReturn(Mono.empty());
        when(eventPublisher.publishOrderUpdated(any(Order.class))).thenReturn(Mono.empty());
        when(eventPublisher.publishOrderCancelled(any(Order.class))).thenReturn(Mono.empty());
        when(eventPublisher.publishOrderConfirmed(any(Order.class), any(BigDecimal.class))).thenReturn(Mono.empty());
        when(eventPublisher.publishPaymentAuthorized(any(Order.class))).thenReturn(Mono.empty());
        when(eventPublisher.publishPaymentRefunded(any(Order.class))).thenReturn(Mono.empty());
        when(eventPublisher.publishPaymentPartiallyRefunded(any(Order.class))).thenReturn(Mono.empty());
        when(eventPublisher.publishOrderEvent(any(OrderEvent.class))).thenReturn(Mono.empty());

        LocalDateTime now = LocalDateTime.now();
        orderDto = new OrderDto(
                1L,
                "CUST-001",
                "customer@example.com",
                OrderDto.OrderStatus.PENDING,
                new BigDecimal("1999.98"),
                Collections.emptyList(),
                Collections.emptyList(),
                now,
                now
        );

        order = new Order();
        order.setId(1L);
        order.setCustomerId("CUST-001");
        order.setStatus("PENDING");
        order.setTotalAmount(new BigDecimal("1999.98"));
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
    }

    @Test
    void getAllOrders_shouldReturnListOfOrders() {
        when(orderRepository.findAll()).thenReturn(Flux.just(order));
        when(orderMapper.toDto(any(Order.class))).thenReturn(orderDto);

        StepVerifier.create(orderService.getAllOrders())
                .expectNextMatches(dto -> dto.id().equals(1L))
                .verifyComplete();

        verify(orderRepository).findAll();
        verify(orderMapper).toDto(order);
    }

    @Test
    void getAllOrders_shouldReturnEmpty_whenNoOrders() {
        when(orderRepository.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(orderService.getAllOrders())
                .verifyComplete();

        verify(orderRepository).findAll();
    }

    @Test
    void getOrderById_shouldReturnOrder_whenExists() {
        when(orderRepository.findById(1L)).thenReturn(Mono.just(order));
        when(orderMapper.toDto(any(Order.class))).thenReturn(orderDto);

        StepVerifier.create(orderService.getOrderById(1L))
                .expectNextMatches(dto -> dto.id().equals(1L))
                .verifyComplete();

        verify(orderRepository).findById(1L);
        verify(orderMapper).toDto(order);
    }

    @Test
    void getOrderById_shouldThrowNotFound_whenNotExists() {
        when(orderRepository.findById(999L)).thenReturn(Mono.empty());

        assertThatThrownBy(() -> orderService.getOrderById(999L).block())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order");

        verify(orderRepository).findById(999L);
    }

    @Test
    void createOrder_shouldCreateOrder_whenValidInput() {
        CreateOrderRequest inputRequest = new CreateOrderRequest(
                "CUST-001",
                "customer@example.com",
                Collections.emptyList()
        );

        OrderDto expectedDto = new OrderDto(
                1L,
                "CUST-001",
                "customer@example.com",
                OrderDto.OrderStatus.PENDING,
                new BigDecimal("1999.98"),
                Collections.emptyList(),
                Collections.emptyList(),
                null,
                null
        );

        when(sagaOrchestrator.createOrder(inputRequest)).thenReturn(Mono.just(expectedDto));

        StepVerifier.create(orderService.createOrder(inputRequest))
                .expectNextMatches(dto -> dto.id().equals(1L) && dto.customerId().equals("CUST-001"))
                .verifyComplete();

        verify(sagaOrchestrator).createOrder(inputRequest);
    }

    @Test
    void createOrder_shouldSetDefaultTotalAmount_whenNull() {
        CreateOrderRequest inputRequest = new CreateOrderRequest(
                "CUST-001",
                "customer@example.com",
                Collections.emptyList()
        );

        OrderDto expectedDto = new OrderDto(
                1L,
                "CUST-001",
                "customer@example.com",
                OrderDto.OrderStatus.PENDING,
                BigDecimal.ZERO,
                Collections.emptyList(),
                Collections.emptyList(),
                null,
                null
        );

        when(sagaOrchestrator.createOrder(inputRequest)).thenReturn(Mono.just(expectedDto));

        StepVerifier.create(orderService.createOrder(inputRequest))
                .expectNextMatches(dto -> dto.totalAmount().equals(BigDecimal.ZERO))
                .verifyComplete();

        verify(sagaOrchestrator).createOrder(inputRequest);
    }
}
