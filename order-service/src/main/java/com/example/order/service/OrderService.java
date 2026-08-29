package com.example.order.service;

import com.example.common.dto.OrderDto;
import com.example.common.exception.ResourceNotFoundException;
import com.example.order.mapper.OrderMapper;
import com.example.order.model.Order;
import com.example.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    @Transactional(readOnly = true)
    public List<com.example.common.dto.OrderDto> getAllOrders() {
        log.debug("Fetching all orders");
        return orderRepository.findAll().stream()
                .map(OrderMapper.INSTANCE::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public com.example.common.dto.OrderDto getOrderById(Long id) {
        log.debug("Fetching order by id: {}", id);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
        return OrderMapper.INSTANCE.toDto(order);
    }

    @Transactional(readOnly = true)
    public List<com.example.common.dto.OrderDto> getOrdersByCustomerId(String customerId) {
        log.debug("Fetching orders by customer id: {}", customerId);
        return orderRepository.findByCustomerId(customerId).stream()
                .map(OrderMapper.INSTANCE::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public com.example.common.dto.OrderDto createOrder(com.example.common.dto.OrderDto orderDto) {
        log.info("Creating order for customer: {}", orderDto.getCustomerId());
        Order order = OrderMapper.INSTANCE.toEntity(orderDto);
        order.setStatus("PENDING");
        if (order.getTotalAmount() == null) {
            order.setTotalAmount(java.math.BigDecimal.ZERO);
        }
        Order saved = orderRepository.save(order);
        return com.example.order.mapper.OrderMapper.INSTANCE.toDto(saved);
    }

    @Transactional
    public com.example.common.dto.OrderDto updateOrder(Long id, com.example.common.dto.OrderDto orderDto) {
        log.info("Updating order id: {}", id);
        Order existing = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));

        existing.setStatus(orderDto.getStatus().name());
        existing.setTotalAmount(orderDto.getTotalAmount());
        existing.setCustomerId(orderDto.getCustomerId());

        Order saved = orderRepository.save(existing);
        return com.example.order.mapper.OrderMapper.INSTANCE.toDto(saved);
    }

    @Transactional
    public void deleteOrder(Long id) {
        log.info("Deleting order id: {}", id);
        if (!orderRepository.existsById(id)) {
            throw new ResourceNotFoundException("Order", id);
        }
        orderRepository.deleteById(id);
    }
}