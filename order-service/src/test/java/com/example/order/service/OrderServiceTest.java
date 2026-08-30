package com.example.order.service;

import com.example.common.dto.OrderDto;
import com.example.common.event.OutboxEventPublisher;
import com.example.common.exception.ResourceNotFoundException;
import com.example.order.mapper.OrderMapper;
import com.example.order.model.Order;
import com.example.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OutboxEventPublisher outboxEventPublisher;

    @InjectMocks
    private OrderService orderService;

    private OrderDto orderDto;
    private Order order;

    @BeforeEach
    void setUp() {
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