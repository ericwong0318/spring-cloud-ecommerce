package com.example.order.service;

import com.example.common.dto.OrderDto;
import com.example.common.dto.OrderItemDto;
import com.example.common.exception.ResourceNotFoundException;
import com.example.order.mapper.OrderMapper;
import com.example.order.model.Order;
import com.example.order.model.OrderItem;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private OrderService orderService;

    private OrderDto orderDto;
    private Order order;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, null, null, orderMapper,
                rabbitTemplate, objectMapper, "order.exchange");

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
        when(orderRepository.findAll()).thenReturn(List.of(order));
        when(orderMapper.toDto(any(Order.class))).thenReturn(orderDto);

        List<OrderDto> result = orderService.getAllOrders();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        verify(orderRepository).findAll();
        verify(orderMapper).toDto(order);
    }

    @Test
    void getAllOrders_shouldReturnEmptyList_whenNoOrders() {
        when(orderRepository.findAll()).thenReturn(Collections.emptyList());

        List<OrderDto> result = orderService.getAllOrders();

        assertThat(result).isEmpty();
        verify(orderRepository).findAll();
    }

    @Test
    void getOrderById_shouldReturnOrder_whenExists() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderMapper.toDto(any(Order.class))).thenReturn(orderDto);

        OrderDto result = orderService.getOrderById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(orderRepository).findById(1L);
        verify(orderMapper).toDto(order);
    }

    @Test
    void getOrderById_shouldThrowException_whenNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found with id: 999");

        verify(orderRepository).findById(999L);
        verifyNoInteractions(orderMapper);
    }

    @Test
    void getOrdersByCustomerId_shouldReturnOrders() {
        when(orderRepository.findByCustomerId("CUST-001")).thenReturn(List.of(order));
        when(orderMapper.toDto(any(Order.class))).thenReturn(orderDto);

        List<OrderDto> result = orderService.getOrdersByCustomerId("CUST-001");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCustomerId()).isEqualTo("CUST-001");
        verify(orderRepository).findByCustomerId("CUST-001");
        verify(orderMapper).toDto(order);
    }

    @Test
    void getOrdersByCustomerId_shouldReturnEmptyList_whenNoOrders() {
        when(orderRepository.findByCustomerId("CUST-999")).thenReturn(Collections.emptyList());

        List<OrderDto> result = orderService.getOrdersByCustomerId("CUST-999");

        assertThat(result).isEmpty();
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
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderMapper.toDto(any(Order.class))).thenReturn(orderDto);

        OrderDto result = orderService.createOrder(inputDto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getCustomerId()).isEqualTo("CUST-001");
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

        when(orderMapper.toEntity(any(OrderDto.class))).thenReturn(newOrder);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderMapper.toDto(any(Order.class))).thenReturn(orderDto);

        OrderDto result = orderService.createOrder(inputDto);

        assertThat(result).isNotNull();
        verify(orderRepository).save(argThat(o -> o.getTotalAmount().equals(BigDecimal.ZERO)));
    }

    @Test
    void createOrder_shouldSetReservedAtTimestampOnItems() {
        OrderItemDto itemDto = OrderItemDto.builder()
                .productId(1L)
                .variantId(1L)
                .quantity(2)
                .price(new BigDecimal("999.99"))
                .build();

        OrderDto inputDto = OrderDto.builder()
                .customerId("CUST-001")
                .status(OrderDto.OrderStatus.PENDING)
                .totalAmount(new BigDecimal("1999.98"))
                .items(List.of(itemDto))
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
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderMapper.toDto(any(Order.class))).thenReturn(orderDto);

        LocalDateTime before = LocalDateTime.now();
        orderService.createOrder(inputDto);
        LocalDateTime after = LocalDateTime.now();

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order captured = orderCaptor.getValue();
        assertThat(captured.getItems()).isNotEmpty();
        OrderItem capturedItem = captured.getItems().get(0);
        assertThat(capturedItem.getReservedAt()).isNotNull();
        assertThat(capturedItem.getReservedAt()).isBetween(before, after);
    }

    @Test
    void createOrder_shouldPublishOrderCreatedEventToRabbitMQ() {
        OrderItemDto itemDto = OrderItemDto.builder()
                .productId(1L)
                .variantId(1L)
                .skuCode("LAPTOP-13-SILVER")
                .quantity(2)
                .price(new BigDecimal("999.99"))
                .build();

        OrderDto inputDto = OrderDto.builder()
                .customerId("CUST-001")
                .status(OrderDto.OrderStatus.PENDING)
                .totalAmount(new BigDecimal("1999.98"))
                .items(List.of(itemDto))
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
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderMapper.toDto(any(Order.class))).thenReturn(orderDto);

        orderService.createOrder(inputDto);

        verify(rabbitTemplate).convertAndSend(eq("order.exchange"), eq("order.created"), anyString());
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

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(updatedOrder);
        when(orderMapper.toDto(any(Order.class))).thenReturn(orderDto);

        OrderDto result = orderService.updateOrder(1L, updateDto);

        assertThat(result).isNotNull();
        verify(orderRepository).findById(1L);
        verify(orderRepository).save(any(Order.class));
        verify(orderMapper).toDto(updatedOrder);
    }

    @Test
    void updateOrder_shouldThrowException_whenNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        OrderDto updateDto = OrderDto.builder()
                .customerId("CUST-001")
                .status(OrderDto.OrderStatus.CONFIRMED)
                .totalAmount(new BigDecimal("2999.98"))
                .items(Collections.emptyList())
                .build();

        assertThatThrownBy(() -> orderService.updateOrder(999L, updateDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found with id: 999");

        verify(orderRepository).findById(999L);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void deleteOrder_shouldDeleteOrder_whenExists() {
        when(orderRepository.existsById(1L)).thenReturn(true);

        orderService.deleteOrder(1L);

        verify(orderRepository).existsById(1L);
        verify(orderRepository).deleteById(1L);
    }

    @Test
    void deleteOrder_shouldThrowException_whenNotFound() {
        when(orderRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> orderService.deleteOrder(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found with id: 999");

        verify(orderRepository).existsById(999L);
        verify(orderRepository, never()).deleteById(anyLong());
    }
}