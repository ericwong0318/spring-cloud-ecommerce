package com.example.order.service;

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

    public OrderSagaOrchestratorImpl(OrderRepository orderRepository,
                                     OrderItemRepository orderItemRepository,
                                     OrderMapper orderMapper,
                                     ObjectMapper objectMapper,
                                     TransactionalOperator transactionalOperator,
                                     OutboxEventPublisher outboxPublisher,
                                     PaymentProcessor paymentProcessor,
                                     OrderEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderMapper = orderMapper;
        this.objectMapper = objectMapper;
        this.transactionalOperator = transactionalOperator;
        this.outboxPublisher = outboxPublisher;
        this.paymentProcessor = paymentProcessor;
        this.eventPublisher = eventPublisher;
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

    private Mono<Void> publishOrderEvent(String eventType, Order order, List<OrderEvent.OrderItem> items) {
        OrderEvent event = switch (eventType) {
            case "ORDER_CANCELLED" -> OrderEvent.cancelled(order.getId(), order.getCustomerId(), order.getCustomerEmail(), items);
            case "ORDER_UPDATED" -> OrderEvent.statusChanged(order.getId(), OrderEvent.OrderStatus.valueOf(order.getStatus()));
            default -> OrderEvent.statusChanged(order.getId(), OrderEvent.OrderStatus.valueOf(order.getStatus()));
        };
        return outboxPublisher.saveEvent("Order", order.getId().toString(), eventType, event);
    }

    @Override
    public Mono<Void> handleInventoryReserved(InventoryEvent event) {
        log.info("Saga orchestrator handling INVENTORY_RESERVED for variant: {}, reserved: {}, backordered: {}",
                event.getVariantId(), event.getReserved(), event.getBackordered());

        if (event.getVariantId() == null) {
            log.warn("Inventory event missing variantId, skipping");
            return Mono.empty();
        }

        int reserved = event.getReserved() != null ? event.getReserved() : 0;
        int backordered = event.getBackordered() != null ? event.getBackordered() : 0;

        if (reserved > 0) {
            return handleStockReserved(event.getVariantId(), reserved, backordered);
        } else if (backordered > 0) {
            return handleStockReservationFailed(event.getVariantId(), backordered);
        }
        return Mono.empty();
    }

    private Mono<Void> handleStockReserved(Long variantId, int reserved, int backordered) {
        return orderItemRepository.findByVariantIdAndStatus(variantId, OrderItem.OrderItemStatus.PENDING)
                .flatMap(orderItem -> {
                    if (orderItem == null) {
                        log.warn("No PENDING OrderItem found for variantId: {}", variantId);
                        return Mono.empty();
                    }

                    if (backordered > 0) {
                        orderItem.setStatus(OrderItem.OrderItemStatus.BACKORDERED);
                        log.info("OrderItem {} set to BACKORDERED (reserved={}, backordered={})", orderItem.getId(), reserved, backordered);
                    } else {
                        orderItem.setStatus(OrderItem.OrderItemStatus.RESERVED);
                        orderItem.setReservedAt(LocalDateTime.now());
                        log.info("OrderItem {} set to RESERVED", orderItem.getId());
                    }
                    return orderItemRepository.save(orderItem)
                            .flatMap(savedItem -> checkAndTransitionOrderToReserved(savedItem.getOrderId()));
                })
                .then();
    }

    private Mono<Void> checkAndTransitionOrderToReserved(Long orderId) {
        return orderRepository.findById(orderId)
                .flatMap(order -> orderItemRepository.findByOrderId(orderId).collectList()
                        .flatMap(items -> {
                            boolean allItemsReservedOrBackordered = items.stream()
                                    .allMatch(item -> item.getStatus() == OrderItem.OrderItemStatus.RESERVED ||
                                            item.getStatus() == OrderItem.OrderItemStatus.BACKORDERED ||
                                            item.getStatus() == OrderItem.OrderItemStatus.SHIPPED);

                            if (allItemsReservedOrBackordered) {
                                order.setStatus("RESERVED");
                                return orderRepository.save(order)
                                        .flatMap(saved -> {
                                            log.info("Order {} transitioned to RESERVED", saved.getId());
                                            OrderEvent updatedEvent = OrderEvent.statusChanged(saved.getId(),
                                                    OrderEvent.OrderStatus.valueOf("RESERVED"));
                                            return outboxPublisher.saveEvent("Order", saved.getId().toString(),
                                                    "ORDER_UPDATED", updatedEvent);
                                        })
                                        .then();
                            }
                            return Mono.empty();
                        }));
    }

    private Mono<Void> handleStockReservationFailed(Long variantId, int backordered) {
        return orderItemRepository.findByVariantIdAndStatus(variantId, OrderItem.OrderItemStatus.PENDING)
                .flatMap(orderItem -> {
                    if (orderItem == null) {
                        log.warn("No PENDING OrderItem found for variantId: {}", variantId);
                        return Mono.empty();
                    }

                    Long orderId = orderItem.getOrderId();

                    orderItem.setStatus(OrderItem.OrderItemStatus.CANCELLED);
                    return orderItemRepository.save(orderItem)
                            .doOnNext(saved -> log.info("OrderItem {} cancelled due to stock reservation failure", saved.getId()))
                            .flatMap(savedItem -> checkAndCancelOrderIfAllItemsCancelledOrBackordered(orderId));
                })
                .then();
    }

    private Mono<Void> checkAndCancelOrderIfAllItemsCancelledOrBackordered(Long orderId) {
        return orderRepository.findById(orderId)
                .flatMap(order -> orderItemRepository.findByOrderId(orderId).collectList()
                        .flatMap(orderItems -> {
                            boolean allItemsCancelledOrBackordered = orderItems.stream()
                                    .allMatch(item -> item.getStatus() == OrderItem.OrderItemStatus.CANCELLED ||
                                            item.getStatus() == OrderItem.OrderItemStatus.BACKORDERED);

                            if (allItemsCancelledOrBackordered) {
                                order.setStatus("CANCELLED");
                                return orderRepository.save(order)
                                        .flatMap(saved -> {
                                            log.info("Order {} cancelled due to stock reservation failure", saved.getId());
                                            return orderItemRepository.findByOrderId(orderId).collectList()
                                                    .flatMap(cancelledItems -> {
                                                        OrderEvent cancelledEvent = OrderEvent.cancelled(saved.getId(), saved.getCustomerId(),
                                                                null, cancelledItems.stream()
                                                                .map(item -> new OrderEvent.OrderItem(
                                                                        item.getId(),
                                                                        item.getProductId(),
                                                                        item.getVariantId(),
                                                                        item.getProductName(),
                                                                        item.getSkuCode(),
                                                                        item.getQuantityOrdered(),
                                                                        item.getQuantityShipped(),
                                                                        item.getUnitPrice(),
                                                                        OrderEvent.OrderItemStatus.valueOf(item.getStatus().name()),
                                                                        item.getReservedAt()))
                                                                .collect(Collectors.toList()));
                                                        return outboxPublisher.saveEvent("Order", saved.getId().toString(),
                                                                "ORDER_CANCELLED", cancelledEvent);
                                                    });
                                        })
                                        .then();
                            }
                            return Mono.empty();
                        }));
    }

    @Override
    public Mono<Void> handleInventoryReleased(InventoryEvent event) {
        log.info("Saga orchestrator handling INVENTORY_RELEASED for variant: {}, quantity: {}",
                event.getVariantId(), event.getReservedQuantity());

        if (event.getVariantId() == null) {
            log.warn("Inventory released event missing variantId, skipping");
            return Mono.empty();
        }

        return orderItemRepository.findByVariantIdAndStatus(event.getVariantId(), OrderItem.OrderItemStatus.RESERVED)
                .flatMap(orderItem -> {
                    if (orderItem == null) {
                        log.warn("No RESERVED OrderItem found for variantId: {}", event.getVariantId());
                        return Mono.empty();
                    }

                    orderItem.setStatus(OrderItem.OrderItemStatus.PENDING);
                    orderItem.setReservedAt(null);
                    return orderItemRepository.save(orderItem)
                            .flatMap(savedItem -> checkAndTransitionOrderToPending(savedItem.getOrderId()));
                })
                .then();
    }

    private Mono<Void> checkAndTransitionOrderToPending(Long orderId) {
        return orderRepository.findById(orderId)
                .flatMap(order -> orderItemRepository.findByOrderId(orderId).collectList()
                        .flatMap(items -> {
                            boolean allItemsPending = items.stream()
                                    .allMatch(item -> item.getStatus() == OrderItem.OrderItemStatus.PENDING ||
                                            item.getStatus() == OrderItem.OrderItemStatus.BACKORDERED);

                            if (allItemsPending) {
                                order.setStatus("PENDING");
                                return orderRepository.save(order)
                                        .flatMap(saved -> {
                                            log.info("Order {} transitioned to PENDING", saved.getId());
                                            return publishOrderEvent("ORDER_UPDATED", saved, toEventItems(saved.getItems()));
                                        })
                                        .then();
                            }
                            return Mono.empty();
                        }));
    }

    @Override
    public Mono<Void> handlePaymentAuthorized(PaymentEvent event) {
        log.info("Saga orchestrator handling PAYMENT_AUTHORIZED for order: {}", event.getOrderId());
        return paymentProcessor.handlePaymentAuthorized(event.getOrderId());
    }

    @Override
    public Mono<Void> handlePaymentCaptured(PaymentEvent event) {
        log.info("Saga orchestrator handling PAYMENT_CAPTURED for order: {}, amount: {}", event.getOrderId(), event.getAmount());
        return paymentProcessor.handlePaymentCaptured(event.getOrderId(), event.getAmount());
    }

    @Override
    public Mono<Void> handlePaymentFailed(PaymentEvent event) {
        log.info("Saga orchestrator handling PAYMENT_FAILED for order: {}", event.getOrderId());
        return paymentProcessor.handlePaymentFailed(event.getOrderId());
    }

    @Override
    public Mono<Void> handlePaymentRefunded(PaymentEvent event) {
        log.info("Saga orchestrator handling PAYMENT_REFUNDED for order: {}", event.getOrderId());
        return paymentProcessor.handlePaymentRefunded(event.getOrderId());
    }

    @Override
    public Mono<Void> handlePaymentPartiallyRefunded(PaymentEvent event) {
        log.info("Saga orchestrator handling PAYMENT_PARTIALLY_REFUNDED for order: {}", event.getOrderId());
        return paymentProcessor.handlePaymentPartiallyRefunded(event.getOrderId());
    }

    @Override
    public Mono<Void> handleReservationExpired(ReservationExpiredEvent event) {
        log.info("Saga orchestrator handling RESERVATION_EXPIRED for orderItemId: {}, variantId: {}, quantityReleased: {}",
                event.getOrderItemId(), event.getVariantId(), event.getQuantityReleased());

        if (event.getOrderItemId() == null) {
            log.warn("ReservationExpiredEvent missing orderItemId, skipping");
            return Mono.empty();
        }

        return orderItemRepository.findById(event.getOrderItemId())
                .switchIfEmpty(Mono.empty())
                .filter(item -> item.getStatus() == OrderItem.OrderItemStatus.RESERVED)
                .flatMap(item -> {
                    item.setStatus(OrderItem.OrderItemStatus.CANCELLED);
                    return orderItemRepository.save(item);
                })
                .flatMap(savedItem -> checkAndCancelOrderIfAllItemsCancelledOrBackordered(savedItem.getOrderId())
                        .thenReturn(savedItem))
                .doOnNext(item -> log.info("OrderItem {} cancelled due to reservation expiry", item.getId()))
                .then();
    }

    @Override
    public Mono<Void> handleOrderCancelled(OrderEvent event) {
        log.info("Saga orchestrator handling ORDER_CANCELLED for order: {}", event.getOrderId());
        return transactionalOperator.transactional(orderRepository.findById(event.getOrderId())
                .switchIfEmpty(Mono.error(new com.example.common.exception.ResourceNotFoundException("Order", event.getOrderId())))
                .flatMap(order -> {
                    if (!OrderEvent.OrderStatus.PENDING.name().equals(order.getStatus()) &&
                            !OrderEvent.OrderStatus.RESERVED.name().equals(order.getStatus())) {
                        log.warn("Order {} cannot be cancelled from status: {}", event.getOrderId(), order.getStatus());
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
                }))
                .then();
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