package com.example.order.service;

import com.example.common.dto.CreateOrderItemRequest;
import com.example.common.dto.CreateOrderRequest;
import com.example.common.dto.OrderDto;
import com.example.common.dto.OrderItemDto;
import com.example.common.event.InventoryEvent;
import com.example.common.event.OrderEvent;
import com.example.common.event.OutboxEventPublisher;
import com.example.common.event.PaymentEvent;
import com.example.common.event.ReservationExpiredEvent;
import com.example.common.saga.SagaOrchestrator;
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
    private final PaymentProcessor paymentProcessor;
    private final OrderEventPublisher eventPublisher;
    private final OrderSagaEventHandlerImpl sagaEventHandler;

    public OrderSagaOrchestratorImpl(OrderRepository orderRepository,
                                     OrderItemRepository orderItemRepository,
                                     OrderMapper orderMapper,
                                     ObjectMapper objectMapper,
                                     TransactionalOperator transactionalOperator,
                                     OutboxEventPublisher outboxPublisher,
                                     PaymentProcessor paymentProcessor,
                                     OrderEventPublisher eventPublisher,
                                     OrderSagaEventHandlerImpl sagaEventHandler) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderMapper = orderMapper;
        this.objectMapper = objectMapper;
        this.transactionalOperator = transactionalOperator;
        this.outboxPublisher = outboxPublisher;
        this.paymentProcessor = paymentProcessor;
        this.eventPublisher = eventPublisher;
        this.sagaEventHandler = sagaEventHandler;
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
    public Mono<OrderDto> createOrder(CreateOrderRequest request) {
        log.info("Creating order for customer: {}", request.customerId());
        Order order = new Order();
        order.setCustomerId(request.customerId());
        order.setCustomerEmail(request.customerEmail());
        order.setStatus(OrderEvent.OrderStatus.PENDING.name());
        order.setTotalAmount(BigDecimal.ZERO);

        LocalDateTime now = LocalDateTime.now();

        if (request.items() != null) {
            for (CreateOrderItemRequest itemDto : request.items()) {
                OrderItem item = new OrderItem();
                item.setProductId(itemDto.productId());
                item.setVariantId(itemDto.variantId());
                item.setQuantityOrdered(itemDto.quantity());
                item.setQuantityShipped(0);
                // Price will be fetched from product service
                item.setUnitPrice(BigDecimal.ZERO);
                item.setStatus(OrderItem.OrderItemStatus.PENDING);
                item.setReservedAt(now);
                order.addItem(item);
            }
        }

        return transactionalOperator.transactional(orderRepository.save(order))
                .flatMap(saved -> {
                    List<OrderEvent.OrderItem> eventItems = toEventItems(saved.getItems());
                    OrderEvent event = OrderEvent.created(saved.getId(), saved.getCustomerId(),
                            request.customerEmail(), saved.getTotalAmount(), eventItems);
                    return outboxPublisher.saveEvent("Order", saved.getId().toString(),
                            "ORDER_CREATED", event)
                            .then(Mono.just(saved));
                })
                .map(orderMapper::toDto);
    }

    @Override
    public Mono<Void> handleOrderCreated(OrderEvent event) {
        log.info("Saga orchestrator handling ORDER_CREATED for order: {}", event.getOrderId());
        return transactionalOperator.transactional(orderRepository.findById(event.getOrderId())
                .switchIfEmpty(Mono.error(new com.example.common.exception.ResourceNotFoundException("Order", event.getOrderId())))
                .flatMap(order -> {
                    if (!OrderEvent.OrderStatus.PENDING.name().equals(order.getStatus())) {
                        log.info("Order {} is not in PENDING status, skipping", event.getOrderId());
                        return Mono.empty();
                    }
                    return Mono.empty();
                }));
    }

    // Delegate event handlers to OrderSagaEventHandlerImpl
    @Override
    public Mono<Void> handleInventoryReserved(InventoryEvent event) {
        return sagaEventHandler.handleInventoryReserved(event);
    }

    @Override
    public Mono<Void> handleInventoryReleased(InventoryEvent event) {
        return sagaEventHandler.handleInventoryReleased(event);
    }

    @Override
    public Mono<Void> handlePaymentAuthorized(PaymentEvent event) {
        return sagaEventHandler.handlePaymentAuthorized(event);
    }

    @Override
    public Mono<Void> handlePaymentCaptured(PaymentEvent event) {
        return sagaEventHandler.handlePaymentCaptured(event);
    }

    @Override
    public Mono<Void> handlePaymentFailed(PaymentEvent event) {
        return sagaEventHandler.handlePaymentFailed(event);
    }

    @Override
    public Mono<Void> handlePaymentRefunded(PaymentEvent event) {
        return sagaEventHandler.handlePaymentRefunded(event);
    }

    @Override
    public Mono<Void> handlePaymentPartiallyRefunded(PaymentEvent event) {
        return sagaEventHandler.handlePaymentPartiallyRefunded(event);
    }

    @Override
    public Mono<Void> handleReservationExpired(ReservationExpiredEvent event) {
        return sagaEventHandler.handleReservationExpired(event);
    }

    @Override
    public Mono<Void> handleOrderCancelled(OrderEvent event) {
        return sagaEventHandler.handleOrderCancelled(event);
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