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
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.transaction.reactive.TransactionCallback;

import com.fasterxml.jackson.core.JsonProcessingException;
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
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final String orderExchange;
    private final String ecommerceExchange;
    private final TransactionalOperator transactionalOperator;

    public OrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                        OrderMapper orderMapper,
                        RabbitTemplate rabbitTemplate, ObjectMapper objectMapper,
                        R2dbcTransactionManager transactionManager,
                        @Value("${rabbitmq.exchange.order}") String orderExchange,
                        @Value("${rabbitmq.exchange.ecommerce}") String ecommerceExchange) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderMapper = orderMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.orderExchange = orderExchange;
        this.ecommerceExchange = ecommerceExchange;
        this.transactionalOperator = TransactionalOperator.create(transactionManager);
    }

    // Test-only constructor
    OrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                 OrderMapper orderMapper,
                 RabbitTemplate rabbitTemplate, ObjectMapper objectMapper,
                 TransactionalOperator transactionalOperator,
                 String orderExchange, String ecommerceExchange) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderMapper = orderMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.orderExchange = orderExchange;
        this.ecommerceExchange = ecommerceExchange;
        this.transactionalOperator = transactionalOperator;
    }

    public Flux<OrderDto> getAllOrders() {
        log.debug("Fetching all orders");
        return orderRepository.findAll()
                .map(orderMapper::toDto);
    }

    public Mono<OrderDto> getOrderById(Long id) {
        log.debug("Fetching order by id: {}", id);
        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Order", id)))
                .map(orderMapper::toDto);
    }

    public Flux<OrderDto> getOrdersByCustomerId(String customerId) {
        log.debug("Fetching orders by customer id: {}", customerId);
        return orderRepository.findByCustomerId(customerId)
                .map(orderMapper::toDto);
    }

    private OrderEvent.OrderItemStatus mapToEventItemStatus(OrderItem.OrderItemStatus status) {
        return OrderEvent.OrderItemStatus.valueOf(status.name());
    }

    private OrderEvent.OrderStatus mapToEventOrderStatus(OrderEvent.OrderStatus status) {
        return status;
    }

    public Mono<OrderDto> createOrder(OrderDto orderDto) {
        log.info("Creating order for customer: {}", orderDto.getCustomerId());
        Order order = orderMapper.toEntity(orderDto);
        order.setStatus(OrderEvent.OrderStatus.PENDING.name());
        if (order.getTotalAmount() == null) {
            order.setTotalAmount(java.math.BigDecimal.ZERO);
        }

        LocalDateTime now = LocalDateTime.now();

        // Create OrderItems from DTO items
        if (orderDto.getItems() != null) {
            for (OrderItemDto itemDto : orderDto.getItems()) {
                OrderItem item = new OrderItem();
                item.setProductId(itemDto.getProductId());
                item.setVariantId(itemDto.getVariantId());
                item.setSkuCode(itemDto.getSkuCode());
                item.setProductName(itemDto.getProductName());
                item.setQuantityOrdered(itemDto.getQuantity());
                item.setQuantityShipped(0);
                item.setUnitPrice(itemDto.getPrice());
                item.setStatus(OrderItem.OrderItemStatus.PENDING);
                item.setReservedAt(now);
                order.addItem(item);
            }
        }

        return transactionalOperator.transactional(orderRepository.save(order))
                .flatMap(saved -> {
                    // Publish OrderEvent.CREATED to RabbitMQ direct
                    List<OrderEvent.OrderItem> eventItems = saved.getItems().stream()
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

                    OrderEvent event = OrderEvent.created(saved.getId(), saved.getCustomerId(),
                            orderDto.getCustomerEmail(), saved.getTotalAmount(), eventItems);
                    try {
                        String jsonPayload = objectMapper.writeValueAsString(event);
                        rabbitTemplate.convertAndSend(orderExchange, "order.created", jsonPayload);
                        log.info("Published OrderEvent.CREATED to RabbitMQ for order: {}", saved.getId());
                    } catch (JsonProcessingException e) {
                        log.error("Failed to serialize OrderEvent for order: {}", saved.getId(), e);
                        throw new RuntimeException("Failed to serialize OrderEvent", e);
                    }
                    return Mono.just(saved);
                })
                .map(orderMapper::toDto);
    }

    public Mono<OrderDto> updateOrder(Long id, OrderDto orderDto) {
        log.info("Updating order id: {}", id);
        return transactionalOperator.transactional(orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Order", id)))
                .flatMap(existing -> {
                    existing.setStatus(orderDto.getStatus().name());
                    existing.setTotalAmount(orderDto.getTotalAmount());
                    existing.setCustomerId(orderDto.getCustomerId());
                    return orderRepository.save(existing);
                })
                .flatMap(saved -> {
                    try {
                        String jsonPayload = objectMapper.writeValueAsString(OrderEvent.statusChanged(saved.getId(), mapToEventOrderStatus(OrderEvent.OrderStatus.valueOf(saved.getStatus()))));
                        rabbitTemplate.convertAndSend(orderExchange, "order.updated", jsonPayload);
                        log.info("Published OrderEvent.UPDATED to RabbitMQ for order: {}", saved.getId());
                    } catch (JsonProcessingException e) {
                        log.error("Failed to serialize OrderEvent for order: {}", saved.getId(), e);
                        throw new RuntimeException("Failed to serialize OrderEvent", e);
                    }
                    return Mono.just(saved);
                })
                .map(orderMapper::toDto));
    }

    public Mono<Void> deleteOrder(Long id) {
        log.info("Deleting order id: {}", id);
        return transactionalOperator.transactional(orderRepository.existsById(id)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new ResourceNotFoundException("Order", id));
                    }
                    return orderRepository.deleteById(id);
                }));
    }

    public void publishOrderEvent(OrderEvent event) {
        log.info("Publishing OrderEvent: eventType={}, orderId={}", event.getEventType(), event.getOrderId());
        try {
            String jsonPayload = objectMapper.writeValueAsString(event);
            String routingKey = switch (event.getEventType()) {
                case "CREATED" -> "order.created";
                case "UPDATED" -> "order.updated";
                case "CANCELLED" -> "order.cancelled";
                case "CONFIRMED" -> "order.confirmed";
                default -> "order.updated";
            };
            rabbitTemplate.convertAndSend(orderExchange, routingKey, jsonPayload);

            // Also publish to ecommerce.events exchange for cross-service communication
            rabbitTemplate.convertAndSend(ecommerceExchange, routingKey, jsonPayload);

            log.info("Published OrderEvent {} to RabbitMQ for order: {}", event.getEventType(), event.getOrderId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize OrderEvent for order: {}", event.getOrderId(), e);
            throw new RuntimeException("Failed to serialize OrderEvent", e);
        }
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

                    // Cancel all items that are not already shipped or cancelled
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
                            .flatMap(saved -> {
                                List<OrderEvent.OrderItem> eventItems = saved.getItems().stream()
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

                                OrderEvent cancelledEvent = OrderEvent.cancelled(saved.getId(), saved.getCustomerId(),
                                        null, eventItems);
                                publishOrderEvent(cancelledEvent);

                                return Mono.just(orderMapper.toDto(saved));
                            });
                }));
    }

    // Saga orchestrator methods

    public Mono<Void> handleInventoryReserved(Long variantId, Integer reservedQuantity, Integer backorderedQuantity) {
        log.info("Handling inventory reserved for variant: {}, reserved={}, backordered={}", variantId, reservedQuantity, backorderedQuantity);

        if (reservedQuantity > 0) {
            return orderItemRepository.findByVariantIdAndStatus(variantId, OrderItem.OrderItemStatus.PENDING)
                    .flatMap(item -> {
                        if (backorderedQuantity > 0) {
                            item.setStatus(OrderItem.OrderItemStatus.BACKORDERED);
                        } else {
                            item.setStatus(OrderItem.OrderItemStatus.RESERVED);
                            item.setReservedAt(LocalDateTime.now());
                        }
                        return orderItemRepository.save(item)
                                .flatMap(savedItem -> checkAndTransitionOrderToReserved(savedItem.getOrderId()));
                    });
        } else if (backorderedQuantity > 0) {
            // Fully backordered - cancel the item
            return orderItemRepository.findByVariantIdAndStatus(variantId, OrderItem.OrderItemStatus.PENDING)
                    .flatMap(item -> {
                        item.setStatus(OrderItem.OrderItemStatus.CANCELLED);
                        return orderItemRepository.save(item)
                                .flatMap(savedItem -> checkAndCancelOrderIfAllItemsCancelledOrBackordered(savedItem.getOrderId()));
                    });
        }
        return Mono.empty();
    }

    public Mono<Void> handlePaymentAuthorized(Long orderId) {
        log.info("Handling payment authorized for order: {}", orderId);
        return transactionalOperator.transactional(orderRepository.findById(orderId)
                .filter(order -> OrderEvent.OrderStatus.PENDING.name().equals(order.getStatus()) ||
                        OrderEvent.OrderStatus.RESERVED.name().equals(order.getStatus()))
                .flatMap(order -> {
                    // Payment authorized - order can proceed to confirmation after capture
                    log.info("Payment authorized for order {}, waiting for capture", orderId);
                    return Mono.empty();
                }));
    }

    public Mono<Void> handlePaymentCaptured(Long orderId, BigDecimal capturedAmount) {
        log.info("Handling payment captured for order: {}, amount={}", orderId, capturedAmount);
        return transactionalOperator.transactional(orderRepository.findById(orderId)
                .filter(order -> OrderEvent.OrderStatus.PENDING.name().equals(order.getStatus()) ||
                        OrderEvent.OrderStatus.RESERVED.name().equals(order.getStatus()))
                .flatMap(order -> {
                    order.setStatus(OrderEvent.OrderStatus.PAID.name());
                    Flux<OrderItem> itemsToUpdate = Flux.fromIterable(order.getItems())
                            .filter(item -> item.getStatus() == OrderItem.OrderItemStatus.PENDING ||
                                    item.getStatus() == OrderItem.OrderItemStatus.RESERVED ||
                                    item.getStatus() == OrderItem.OrderItemStatus.BACKORDERED)
                            .flatMap(item -> {
                                item.setStatus(OrderItem.OrderItemStatus.RESERVED);
                                return orderItemRepository.save(item);
                            });

                    return itemsToUpdate
                            .then(orderRepository.save(order))
                            .flatMap(saved -> {
                                List<OrderEvent.OrderItem> eventItems = saved.getItems().stream()
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

                                OrderEvent confirmedEvent = OrderEvent.confirmed(order.getId(), order.getCustomerId(),
                                        null, order.getTotalAmount(), eventItems);
                                publishOrderEvent(confirmedEvent);

                                return Mono.empty();
                            });
                })
                .switchIfEmpty(Mono.empty()));
    }

    public Mono<Void> handlePaymentFailed(Long orderId) {
        log.info("Handling payment failed for order: {}", orderId);
        return transactionalOperator.transactional(orderRepository.findById(orderId)
                .filter(order -> OrderEvent.OrderStatus.PENDING.name().equals(order.getStatus()) ||
                        OrderEvent.OrderStatus.RESERVED.name().equals(order.getStatus()))
                .flatMap(order -> {
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
                            .flatMap(saved -> {
                                List<OrderEvent.OrderItem> eventItems = saved.getItems().stream()
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

                                OrderEvent cancelledEvent = OrderEvent.cancelled(order.getId(), order.getCustomerId(),
                                        null, eventItems);
                                publishOrderEvent(cancelledEvent);

                                return Mono.empty();
                            });
                })
                .switchIfEmpty(Mono.empty());
    }

    public Mono<Void> handleReservationExpired(Long orderItemId) {
        log.info("Handling reservation expired for orderItem: {}", orderItemId);
        return orderItemRepository.findById(orderItemId)
                .filter(item -> item.getStatus() == OrderItem.OrderItemStatus.RESERVED)
                .flatMap(item -> {
                    item.setStatus(OrderItem.OrderItemStatus.CANCELLED);
                    return orderItemRepository.save(item)
                            .flatMap(savedItem -> checkAndCancelOrderIfAllItemsCancelledOrBackordered(savedItem.getOrderId()));
                })
                .switchIfEmpty(Mono.empty());
    }

    public Mono<Void> handleReservationExpired(Long orderItemId) {
        log.info("Handling reservation expired for orderItem: {}", orderItemId);
        return transactionalOperator.transactional(orderItemRepository.findById(orderItemId)
                .filter(item -> item.getStatus() == OrderItem.OrderItemStatus.RESERVED)
                .flatMap(item -> {
                    item.setStatus(OrderItem.OrderItemStatus.CANCELLED);
                    return orderItemRepository.save(item)
                            .flatMap(savedItem -> checkAndCancelOrderIfAllItemsCancelledOrBackordered(savedItem.getOrderId()));
                })
                .switchIfEmpty(Mono.empty()));
    }

    private Mono<Void> checkAndTransitionOrderToReserved(Long orderId) {
        return transactionalOperator.transactional(orderRepository.findById(orderId)
                .filter(order -> OrderEvent.OrderStatus.PENDING.name().equals(order.getStatus()))
                .flatMap(order -> {
                    return Flux.fromIterable(order.getItems())
                            .all(item -> item.getStatus() == OrderItem.OrderItemStatus.RESERVED ||
                                    item.getStatus() == OrderItem.OrderItemStatus.BACKORDERED ||
                                    item.getStatus() == OrderItem.OrderItemStatus.SHIPPED)
                            .flatMap(allReservedOrBackordered -> {
                                if (allReservedOrBackordered) {
                                    order.setStatus(OrderEvent.OrderStatus.RESERVED.name());
                                    return orderRepository.save(order)
                                            .flatMap(saved -> {
                                                OrderEvent updatedEvent = OrderEvent.statusChanged(saved.getId(), OrderEvent.OrderStatus.RESERVED);
                                                publishOrderEvent(updatedEvent);
                                                return Mono.empty();
                                            });
                                }
                                return Mono.empty();
                            });
                })
                .switchIfEmpty(Mono.empty()));
    }

    private Mono<Void> checkAndCancelOrderIfAllItemsCancelledOrBackordered(Long orderId) {
        return transactionalOperator.transactional(orderRepository.findById(orderId)
                .flatMap(order -> {
                    return Flux.fromIterable(order.getItems())
                            .all(item -> item.getStatus() == OrderItem.OrderItemStatus.CANCELLED ||
                                    item.getStatus() == OrderItem.OrderItemStatus.BACKORDERED)
                            .flatMap(allCancelledOrBackordered -> {
                                if (allCancelledOrBackordered) {
                                    order.setStatus(OrderEvent.OrderStatus.CANCELLED.name());
                                    return orderRepository.save(order)
                                            .flatMap(saved -> {
                                                List<OrderEvent.OrderItem> eventItems = saved.getItems().stream()
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

                                                OrderEvent cancelledEvent = OrderEvent.cancelled(saved.getId(), saved.getCustomerId(),
                                                        null, eventItems);
                                                publishOrderEvent(cancelledEvent);

                                                return Mono.empty();
                                            });
                                }
                                return Mono.empty();
                            });
                })
                .switchIfEmpty(Mono.empty()));
    }
}