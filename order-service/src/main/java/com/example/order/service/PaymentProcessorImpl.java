package com.example.order.service;

import com.example.common.event.OrderEvent;
import com.example.common.event.OutboxEventPublisher;
import com.example.common.exception.ResourceNotFoundException;
import com.example.order.model.Order;
import com.example.order.model.OrderItem;
import com.example.order.repository.OrderItemRepository;
import com.example.order.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class PaymentProcessorImpl implements PaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessorImpl.class);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OutboxEventPublisher outboxPublisher;
    private final TransactionalOperator transactionalOperator;
    private final ObjectMapper objectMapper;

    public PaymentProcessorImpl(OrderRepository orderRepository,
                                OrderItemRepository orderItemRepository,
                                OutboxEventPublisher outboxPublisher,
                                TransactionalOperator transactionalOperator,
                                ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.outboxPublisher = outboxPublisher;
        this.transactionalOperator = transactionalOperator;
        this.objectMapper = objectMapper;
    }

    private List<OrderEvent.OrderItem> toEventItems(List<OrderItem> items) {
        return items.stream()
                .map(item -> new OrderEvent.OrderItem(
                        item.getId(),
                        item.getProductId(),
                        item.getVariantId(),
                        item.getProductName(),
                        item.getSkuCode(),
                        item.getQuantityOrdered(),
                        item.getQuantityShipped(),
                        item.getUnitPrice(),
                        mapToEventItemStatus(item.getStatus()),
                        item.getReservedAt()))
                .collect(Collectors.toList());
    }

    private OrderEvent.OrderItemStatus mapToEventItemStatus(OrderItem.OrderItemStatus status) {
        return switch (status) {
            case OrderItem.OrderItemStatus.PENDING -> OrderEvent.OrderItemStatus.PENDING;
            case OrderItem.OrderItemStatus.RESERVED -> OrderEvent.OrderItemStatus.RESERVED;
            case OrderItem.OrderItemStatus.SHIPPED -> OrderEvent.OrderItemStatus.SHIPPED;
            case OrderItem.OrderItemStatus.BACKORDERED -> OrderEvent.OrderItemStatus.BACKORDERED;
            case OrderItem.OrderItemStatus.PARTIALLY_CONFIRMED -> OrderEvent.OrderItemStatus.PARTIALLY_CONFIRMED;
            case OrderItem.OrderItemStatus.CANCELLED -> OrderEvent.OrderItemStatus.CANCELLED;
            default -> OrderEvent.OrderItemStatus.PENDING;
        };
    }

    @Override
    @CircuitBreaker(name = "payment", fallbackMethod = "processRefundFallback")
    @Retry(name = "payment")
    @TimeLimiter(name = "payment")
    public Mono<Void> processRefund(Long orderId) {
        log.info("Processing refund for order: {}", orderId);
        return orderRepository.findById(orderId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Order", orderId)))
                .flatMap(order -> {
                    if (!OrderEvent.OrderStatus.CONFIRMED.name().equals(order.getStatus()) &&
                            !OrderEvent.OrderStatus.SHIPPED.name().equals(order.getStatus()) &&
                            !OrderEvent.OrderStatus.DELIVERED.name().equals(order.getStatus())) {
                        log.warn("Order {} cannot be refunded from status: {}", orderId, order.getStatus());
                        return Mono.error(new IllegalStateException("Order cannot be refunded from status: " + order.getStatus()));
                    }

                    order.setStatus(OrderEvent.OrderStatus.REFUNDED.name());

                    for (OrderItem item : order.getItems()) {
                        if (item.getStatus() == OrderItem.OrderItemStatus.RESERVED) {
                            item.setStatus(OrderItem.OrderItemStatus.CANCELLED);
                            orderItemRepository.save(item).subscribe();
                        }
                    }

                    log.info("Order {} partially refunded, reserved items cancelled", orderId);

                    OrderEvent updatedEvent = OrderEvent.statusChanged(orderId, OrderEvent.OrderStatus.valueOf(order.getStatus()));
                    return outboxPublisher.saveEvent("Order", orderId.toString(),
                            "ORDER_UPDATED", updatedEvent);
                })
                .switchIfEmpty(Mono.empty())
                .<Void>map(v -> null)
                .as(transactionalOperator::transactional);
    }

    private Mono<Void> processRefundFallback(Long orderId, Exception ex) {
        log.error("Fallback triggered for processRefund order {}: {}", orderId, ex.getMessage());
        return Mono.error(new RuntimeException("Payment service unavailable, refund processing failed", ex));
    }

    @Override
    public Mono<Void> handlePaymentAuthorized(Long orderId) {
        log.info("Handling payment authorized for order: {}", orderId);
        return transactionalOperator.transactional(orderRepository.findById(orderId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Order", orderId)))
                .flatMap(order -> {
                    order.setStatus(OrderEvent.OrderStatus.RESERVED.name());
                    return orderRepository.save(order);
                })
                .flatMap(saved -> {
                    OrderEvent event = OrderEvent.statusChanged(saved.getId(), OrderEvent.OrderStatus.RESERVED);
                    return outboxPublisher.saveEvent("Order", saved.getId().toString(),
                            "PAYMENT_AUTHORIZED", event);
                }));
    }

    @Override
    public Mono<Void> handlePaymentCaptured(Long orderId, BigDecimal amount) {
        log.info("Handling payment captured for order: {}", orderId);
        return transactionalOperator.transactional(orderRepository.findById(orderId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Order", orderId)))
                .flatMap(order -> {
                    order.setStatus(OrderEvent.OrderStatus.CONFIRMED.name());
                    return orderRepository.save(order);
                })
                .flatMap(saved -> {
                    OrderEvent event = OrderEvent.confirmed(saved.getId(), saved.getCustomerId(),
                            saved.getCustomerEmail(), amount, toEventItems(saved.getItems()));
                    return outboxPublisher.saveEvent("Order", saved.getId().toString(),
                            "ORDER_CONFIRMED", event);
                }));
    }

    @Override
    public Mono<Void> handlePaymentFailed(Long orderId) {
        log.info("Handling payment failed for order: {}", orderId);
        return transactionalOperator.transactional(orderRepository.findById(orderId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Order", orderId)))
                .flatMap(order -> {
                    order.setStatus(OrderEvent.OrderStatus.CANCELLED.name());
                    return orderRepository.save(order);
                })
                .flatMap(saved -> {
                    OrderEvent event = OrderEvent.cancelled(saved.getId(), saved.getCustomerId(),
                            saved.getCustomerEmail(), toEventItems(saved.getItems()));
                    return outboxPublisher.saveEvent("Order", saved.getId().toString(),
                            "ORDER_CANCELLED", event);
                }));
    }

    @Override
    public Mono<Void> handlePaymentRefunded(Long orderId) {
        log.info("Handling payment refunded for order: {}", orderId);
        return transactionalOperator.transactional(orderRepository.findById(orderId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Order", orderId)))
                .flatMap(order -> {
                    order.setStatus(OrderEvent.OrderStatus.REFUNDED.name());
                    return orderRepository.save(order);
                })
                .flatMap(saved -> {
                    OrderEvent event = OrderEvent.refunded(saved.getId(), saved.getCustomerId(),
                            saved.getCustomerEmail(), saved.getTotalAmount(), toEventItems(saved.getItems()));
                    return outboxPublisher.saveEvent("Order", saved.getId().toString(),
                            "PAYMENT_REFUNDED", event);
                }));
    }

    @Override
    public Mono<Void> handlePaymentPartiallyRefunded(Long orderId) {
        log.info("Handling payment partially refunded for order: {}", orderId);
        return transactionalOperator.transactional(orderRepository.findById(orderId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Order", orderId)))
                .flatMap(order -> {
                    order.setStatus(OrderEvent.OrderStatus.PARTIALLY_REFUNDED.name());
                    return orderRepository.save(order);
                })
                .flatMap(saved -> {
                    OrderEvent event = OrderEvent.partiallyRefunded(saved.getId(), saved.getCustomerId(),
                            saved.getCustomerEmail(), saved.getTotalAmount(), toEventItems(saved.getItems()));
                    return outboxPublisher.saveEvent("Order", saved.getId().toString(),
                            "PAYMENT_PARTIALLY_REFUNDED", event);
                }));
    }
}