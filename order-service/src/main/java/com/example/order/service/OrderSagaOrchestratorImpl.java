package com.example.order.service;

import com.example.common.dto.OrderDto;
import com.example.common.dto.OrderItemDto;
import com.example.common.event.OrderEvent;
import com.example.common.event.OutboxEventPublisher;
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
public class OrderSagaOrchestratorImpl implements OrderSagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaOrchestratorImpl.class);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderMapper orderMapper;
    private final ObjectMapper objectMapper;
    private final TransactionalOperator transactionalOperator;
    private final OutboxEventPublisher outboxPublisher;

    public OrderSagaOrchestratorImpl(OrderRepository orderRepository,
                                     OrderItemRepository orderItemRepository,
                                     OrderMapper orderMapper,
                                     ObjectMapper objectMapper,
                                     TransactionalOperator transactionalOperator,
                                     OutboxEventPublisher outboxPublisher) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderMapper = orderMapper;
        this.objectMapper = objectMapper;
        this.transactionalOperator = transactionalOperator;
        this.outboxPublisher = outboxPublisher;
    }

    private OrderEvent.OrderStatus mapToEventOrderStatus(OrderEvent.OrderStatus status) {
        return status;
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
    public Mono<OrderDto> createOrder(OrderDto orderDto) {
        log.info("Creating order for customer: {}", orderDto.customerId());
        Order order = orderMapper.toEntity(orderDto);
        order.setStatus(OrderEvent.OrderStatus.PENDING.name());
        if (order.getTotalAmount() == null) {
            order.setTotalAmount(BigDecimal.ZERO);
        }

        LocalDateTime now = LocalDateTime.now();

        if (orderDto.items() != null) {
            for (OrderItemDto itemDto : orderDto.items()) {
                OrderItem item = new OrderItem();
                item.setProductId(itemDto.productId());
                item.setVariantId(itemDto.variantId());
                item.setSkuCode(itemDto.skuCode());
                item.setProductName(itemDto.productName());
                item.setQuantityOrdered(itemDto.quantity());
                item.setQuantityShipped(0);
                item.setUnitPrice(itemDto.price());
                item.setStatus(OrderItem.OrderItemStatus.PENDING);
                item.setReservedAt(now);
                order.addItem(item);
            }
        }

        return transactionalOperator.transactional(orderRepository.save(order))
                .flatMap(saved -> {
                    List<OrderEvent.OrderItem> eventItems = toEventItems(saved.getItems());
                    OrderEvent event = OrderEvent.created(saved.getId(), saved.getCustomerId(),
                            orderDto.customerEmail(), saved.getTotalAmount(), eventItems);
                    return outboxPublisher.saveEvent("Order", saved.getId().toString(),
                            "ORDER_CREATED", event)
                            .then(Mono.just(saved));
                })
                .map(orderMapper::toDto);
    }

    @Override
    public Mono<Void> handleReservationExpiry(Long orderId) {
        log.info("Handling reservation expiry for order: {}", orderId);
        return transactionalOperator.transactional(orderRepository.findById(orderId)
                .switchIfEmpty(Mono.error(new com.example.common.exception.ResourceNotFoundException("Order", orderId)))
                .flatMap(order -> {
                    if (!OrderEvent.OrderStatus.RESERVED.name().equals(order.getStatus())) {
                        log.info("Order {} is not in RESERVED status, skipping reservation expiry", orderId);
                        return Mono.empty();
                    }

                    order.setStatus(OrderEvent.OrderStatus.CANCELLED.name());

                    Flux<OrderItem> itemsToUpdate = Flux.fromIterable(order.getItems())
                            .filter(item -> item.getStatus() == OrderItem.OrderItemStatus.RESERVED)
                            .flatMap(item -> {
                                item.setStatus(OrderItem.OrderItemStatus.CANCELLED);
                                return orderItemRepository.save(item);
                            });

                    return itemsToUpdate
                            .then(orderRepository.save(order))
                            .flatMap(saved -> {
                                List<OrderEvent.OrderItem> eventItems = toEventItems(saved.getItems());
                                OrderEvent event = OrderEvent.cancelled(saved.getId(), saved.getCustomerId(),
                                        saved.getCustomerEmail(), eventItems);
                                return outboxPublisher.saveEvent("Order", saved.getId().toString(),
                                        "ORDER_CANCELLED", event)
                                        .then(Mono.empty());
                            });
                }));
    }
}