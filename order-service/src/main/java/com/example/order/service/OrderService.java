package com.example.order.service;

import com.example.common.dto.OrderDto;
import com.example.common.dto.OrderItemDto;
import com.example.common.event.OrderEvent;
import com.example.common.exception.ResourceNotFoundException;
import com.example.order.mapper.OrderMapper;
import com.example.order.model.Order;
import com.example.order.model.OrderItem;
import com.example.order.repository.OrderItemRepository;
import com.example.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderMapper orderMapper;
    private final ObjectMapper objectMapper;
    private final TransactionalOperator transactionalOperator;
    private final OrderSagaOrchestrator sagaOrchestrator;
    private final PaymentProcessor paymentProcessor;
    private final OrderEventPublisher eventPublisher;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        OrderMapper orderMapper,
                        ObjectMapper objectMapper,
                        TransactionalOperator transactionalOperator,
                        OrderSagaOrchestrator sagaOrchestrator,
                        PaymentProcessor paymentProcessor,
                        OrderEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderMapper = orderMapper;
        this.objectMapper = objectMapper;
        this.transactionalOperator = transactionalOperator;
        this.sagaOrchestrator = sagaOrchestrator;
        this.paymentProcessor = paymentProcessor;
        this.eventPublisher = eventPublisher;
    }

    // Test-only constructor (package-private for testing)
    OrderService(OrderRepository orderRepository,
                 OrderItemRepository orderItemRepository,
                 OrderMapper orderMapper,
                 ObjectMapper objectMapper,
                 TransactionalOperator transactionalOperator,
                 OrderSagaOrchestrator sagaOrchestrator,
                 PaymentProcessor paymentProcessor,
                 OrderEventPublisher eventPublisher,
                 boolean testMode) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderMapper = orderMapper;
        this.objectMapper = objectMapper;
        this.transactionalOperator = transactionalOperator;
        this.sagaOrchestrator = sagaOrchestrator;
        this.paymentProcessor = paymentProcessor;
        this.eventPublisher = eventPublisher;
    }

    public Mono<OrderDto> createOrder(OrderDto orderDto) {
        return sagaOrchestrator.createOrder(orderDto);
    }

    public Mono<OrderDto> updateOrder(Long id, OrderDto orderDto) {
        log.info("Updating order id: {}", id);
        return transactionalOperator.transactional(orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Order", id)))
                .flatMap(existing -> {
                    existing.setStatus(orderDto.status().name());
                    existing.setTotalAmount(orderDto.totalAmount());
                    existing.setCustomerId(orderDto.customerId());
                    return orderRepository.save(existing);
                })
                .flatMap(saved -> eventPublisher.publishOrderUpdated(saved).then(Mono.just(saved)))
                .map(orderMapper::toDto));
    }

    public Mono<OrderDto> cancelOrder(Long id) {
        log.info("Cancelling order id: {}", id);
        return transactionalOperator.transactional(orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Order", id)))
                .flatMap(order -> {
                    if (!OrderEvent.OrderStatus.PENDING.name().equals(order.getStatus()) &&
                            !OrderEvent.OrderStatus.RESERVED.name().equals(order.getStatus())) {
                        log.warn("Order {} cannot be cancelled from status: {}", id, order.getStatus());
                        return Mono.error(new IllegalStateException("Order cannot be cancelled from status: " + order.getStatus()));
                    }

                    order.setStatus(OrderEvent.OrderStatus.CANCELLED.name());

                    Flux<OrderItem> itemsToUpdate = Flux.fromIterable(order.getItems())
                            .filter(item -> item.getStatus() == OrderItem.OrderItemStatus.PENDING ||
                                    item.getStatus() == OrderItem.OrderItemStatus.RESERVED ||
                                    item.getStatus() == OrderItem.OrderItemStatus.BACKORDERED)
                            .flatMap(item -> {
                                item.setStatus(OrderItem.OrderItemStatus.CANCELLED);
                                return orderItemRepository.save(item);
                            });

                    return itemsToUpdate
                            .then(orderRepository.save(order))
                            .flatMap(saved -> eventPublisher.publishOrderCancelled(saved).then(Mono.just(saved)))
                            .map(orderMapper::toDto);
                }));
    }

    public Flux<OrderDto> getAllOrders() {
        log.debug("Fetching all orders");
        return orderRepository.findAll()
                .map(orderMapper::toDto);
    }

    public Mono<OrderDto> getOrderById(Long id) {
        log.debug("Fetching order by id: {}", id);
        return orderRepository.findById(id)
                .map(orderMapper::toDto)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Order", id)));
    }

    public Flux<OrderDto> getOrdersByCustomerId(String customerId) {
        log.debug("Fetching orders by customer id: {}", customerId);
        return orderRepository.findByCustomerId(customerId)
                .map(orderMapper::toDto);
    }

    public Mono<Void> deleteOrder(Long id) {
        log.info("Deleting order id: {}", id);
        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Order", id)))
                .flatMap(orderRepository::delete);
    }

    public Mono<OrderDto> updateOrderStatus(Long id, String status) {
        log.info("Updating order {} status to {}", id, status);
        return transactionalOperator.transactional(orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Order", id)))
                .flatMap(order -> {
                    order.setStatus(status);
                    return orderRepository.save(order);
                })
                .flatMap(saved -> eventPublisher.publishOrderUpdated(saved).then(Mono.just(saved)))
                .map(orderMapper::toDto));
    }

    public Mono<Void> handleReservationExpiry(Long orderId) {
        return sagaOrchestrator.handleReservationExpiry(orderId);
    }

    public Mono<Void> processRefund(Long orderId) {
        return paymentProcessor.processRefund(orderId);
    }

    public Mono<Void> handlePaymentAuthorized(Long orderId) {
        return paymentProcessor.handlePaymentAuthorized(orderId);
    }

    public Mono<Void> handlePaymentCaptured(Long orderId, BigDecimal amount) {
        return paymentProcessor.handlePaymentCaptured(orderId, amount);
    }

    public Mono<Void> handlePaymentFailed(Long orderId) {
        return paymentProcessor.handlePaymentFailed(orderId);
    }

    public Mono<Void> handlePaymentRefunded(Long orderId) {
        return paymentProcessor.handlePaymentRefunded(orderId);
    }

    public Mono<Void> handlePaymentPartiallyRefunded(Long orderId) {
        return paymentProcessor.handlePaymentPartiallyRefunded(orderId);
    }

    public Mono<Void> publishOrderEvent(OrderEvent event) {
        return eventPublisher.publishOrderEvent(event);
    }
}