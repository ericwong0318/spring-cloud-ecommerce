package com.example.order.service;

import com.example.common.dto.OrderDto;
import com.example.common.dto.OrderItemDto;
import com.example.common.exception.ResourceNotFoundException;
import com.example.common.event.OrderEvent;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock(lenient = true)
    private OrderRepository orderRepository;

    @Mock(lenient = true)
    private OrderItemRepository orderItemRepository;

    @Mock(lenient = true)
    private OrderMapper orderMapper;

    @Mock(lenient = true)
    private RabbitTemplate rabbitTemplate;

    @Mock(lenient = true)
    private TransactionalOperator transactionalOperator;

    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private OrderService orderService;

    private OrderDto orderDto;
    private Order order;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, orderItemRepository, orderMapper,
                rabbitTemplate, new ObjectMapper().registerModule(new JavaTimeModule()),
                transactionalOperator, "order.exchange", "ecommerce.events");

        // Mock TransactionalOperator to pass through the publisher (no actual transaction in tests)
        when(transactionalOperator.transactional(any(Mono.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionalOperator.transactional(any(Flux.class))).thenAnswer(inv -> inv.getArgument(0));

        orderDto = OrderDto.builder()
                .id(1L)
                .customerId("CUST-001")
                .customerEmail("customer@example.com")
                .status(OrderDto.OrderStatus.PENDING)
                .totalAmount(new BigDecimal("1999.98"))
                .items(Collections.emptyList())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        order = new Order();
        order.setId(1L);
        order.setCustomerId("CUST-001");
        order.setStatus("PENDING");
        order.setTotalAmount(new BigDecimal("1999.98"));
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void getAllOrders_shouldReturnListOfOrders() {
        when(orderRepository.findAll()).thenReturn(Flux.just(order));
        when(orderMapper.toDto(any(Order.class))).thenReturn(orderDto);

        StepVerifier.create(orderService.getAllOrders())
                .expectNextMatches(dto -> dto.getId().equals(1L))
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
                .expectNextMatches(dto -> dto.getId().equals(1L))
                .verifyComplete();

        verify(orderRepository).findById(1L);
        verify(orderMapper).toDto(order);
    }

    @Test
    void getOrderById_shouldThrowException_whenNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Mono.empty());

        StepVerifier.create(orderService.getOrderById(999L))
                .expectErrorMatches(e -> e instanceof ResourceNotFoundException &&
                        e.getMessage().contains("Order not found with id: 999"))
                .verify();

        verify(orderRepository).findById(999L);
        verifyNoInteractions(orderMapper);
    }

    @Test
    void getOrdersByCustomerId_shouldReturnOrders() {
        when(orderRepository.findByCustomerId("CUST-001")).thenReturn(Flux.just(order));
        when(orderMapper.toDto(any(Order.class))).thenReturn(orderDto);

        StepVerifier.create(orderService.getOrdersByCustomerId("CUST-001"))
                .expectNextMatches(dto -> dto.getCustomerId().equals("CUST-001"))
                .verifyComplete();

        verify(orderRepository).findByCustomerId("CUST-001");
        verify(orderMapper).toDto(order);
    }

    @Test
    void getOrdersByCustomerId_shouldReturnEmpty_whenNoOrders() {
        when(orderRepository.findByCustomerId("CUST-999")).thenReturn(Flux.empty());

        StepVerifier.create(orderService.getOrdersByCustomerId("CUST-999"))
                .verifyComplete();

        verify(orderRepository).findByCustomerId("CUST-999");
    }

    @Test
    void createOrder_shouldCreateAndReturnOrder() {
        OrderDto inputDto = OrderDto.builder()
                .customerId("CUST-001")
                .status(OrderDto.OrderStatus.PENDING)
                .totalAmount(new BigDecimal("1999.98"))
                .items(Collections.emptyList())
                .build();

        Order newOrder = new Order();
        newOrder.setCustomerId("CUST-001");
        newOrder.setStatus("PENDING");
        newOrder.setTotalAmount(new BigDecimal("1999.98"));

        Order savedOrder = new Order();
        savedOrder.setId(1L);
        savedOrder.setCustomerId("CUST-001");
        savedOrder.setStatus("PENDING");
        savedOrder.setTotalAmount(new BigDecimal("1999.98"));
        savedOrder.setCreatedAt(LocalDateTime.now());
        savedOrder.setUpdatedAt(LocalDateTime.now());

        when(orderMapper.toEntity(any(OrderDto.class))).thenReturn(newOrder);
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(savedOrder));
        when(orderMapper.toDto(any(Order.class))).thenReturn(orderDto);

        StepVerifier.create(orderService.createOrder(inputDto))
                .expectNextMatches(dto -> dto.getId().equals(1L) && dto.getCustomerId().equals("CUST-001"))
                .verifyComplete();

        verify(orderMapper).toEntity(inputDto);
        verify(orderRepository).save(any(Order.class));
        verify(orderMapper).toDto(savedOrder);
    }

    @Test
    void createOrder_shouldSetDefaultTotalAmount_whenNull() {
        OrderDto inputDto = OrderDto.builder()
                .customerId("CUST-001")
                .status(OrderDto.OrderStatus.PENDING)
                .totalAmount(null)
                .items(Collections.emptyList())
                .build();

        Order newOrder = new Order();
        newOrder.setCustomerId("CUST-001");
        newOrder.setStatus("PENDING");
        newOrder.setTotalAmount(null);

        Order savedOrder = new Order();
        savedOrder.setId(1L);
        savedOrder.setCustomerId("CUST-001");
        savedOrder.setStatus("PENDING");
        savedOrder.setTotalAmount(BigDecimal.ZERO);
        savedOrder.setCreatedAt(LocalDateTime.now());
        savedOrder.setUpdatedAt(LocalDateTime.now());

        OrderDto savedOrderDto = OrderDto.builder()
                .id(1L)
                .customerId("CUST-001")
                .customerEmail("customer@example.com")
                .status(OrderDto.OrderStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .items(Collections.emptyList())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(orderMapper.toEntity(any(OrderDto.class))).thenReturn(newOrder);
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(savedOrder));
        when(orderMapper.toDto(any(Order.class))).thenReturn(savedOrderDto);

        StepVerifier.create(orderService.createOrder(inputDto))
                .expectNextMatches(dto -> dto.getTotalAmount().equals(BigDecimal.ZERO))
                .verifyComplete();

        verify(orderRepository).save(argThat(o -> o.getTotalAmount().equals(BigDecimal.ZERO)));
    }

    @Test
    void updateOrder_shouldUpdateAndReturnOrder() {
        OrderDto updateDto = OrderDto.builder()
                .id(1L)
                .customerId("CUST-001")
                .status(OrderDto.OrderStatus.CONFIRMED)
                .totalAmount(new BigDecimal("2999.98"))
                .items(Collections.emptyList())
                .build();

        Order updatedOrder = new Order();
        updatedOrder.setId(1L);
        updatedOrder.setCustomerId("CUST-001");
        updatedOrder.setStatus("CONFIRMED");
        updatedOrder.setTotalAmount(new BigDecimal("2999.98"));
        updatedOrder.setCreatedAt(LocalDateTime.now());
        updatedOrder.setUpdatedAt(LocalDateTime.now());

        when(orderRepository.findById(1L)).thenReturn(Mono.just(order));
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(updatedOrder));
        when(orderMapper.toDto(any(Order.class))).thenReturn(orderDto);

        StepVerifier.create(orderService.updateOrder(1L, updateDto))
                .expectNextMatches(dto -> dto.getId().equals(1L))
                .verifyComplete();

        verify(orderRepository).findById(1L);
        verify(orderRepository).save(any(Order.class));
        verify(orderMapper).toDto(updatedOrder);
    }

    @Test
    void updateOrder_shouldThrowException_whenNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Mono.empty());

        OrderDto updateDto = OrderDto.builder()
                .customerId("CUST-001")
                .status(OrderDto.OrderStatus.CONFIRMED)
                .totalAmount(new BigDecimal("2999.98"))
                .items(Collections.emptyList())
                .build();

        StepVerifier.create(orderService.updateOrder(999L, updateDto))
                .expectErrorMatches(e -> e instanceof ResourceNotFoundException &&
                        e.getMessage().contains("Order not found with id: 999"))
                .verify();

        verify(orderRepository).findById(999L);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void deleteOrder_shouldDeleteOrder_whenExists() {
        when(orderRepository.existsById(1L)).thenReturn(Mono.just(true));
        when(orderRepository.deleteById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(orderService.deleteOrder(1L))
                .verifyComplete();

        verify(orderRepository).existsById(1L);
        verify(orderRepository).deleteById(1L);
    }

    @Test
    void deleteOrder_shouldThrowException_whenNotFound() {
        when(orderRepository.existsById(999L)).thenReturn(Mono.just(false));

        StepVerifier.create(orderService.deleteOrder(999L))
                .expectErrorMatches(e -> e instanceof ResourceNotFoundException &&
                        e.getMessage().contains("Order not found with id: 999"))
                .verify();

        verify(orderRepository).existsById(999L);
        verify(orderRepository, never()).deleteById(anyLong());
    }
}
