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
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final ObjectMapper objectMapper;
    private final TransactionalOperator transactionalOperator;
    private final RabbitTemplate rabbitTemplate;
    private final String orderExchange;
    private final String ecommerceExchange;

    @Autowired
    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        OrderMapper orderMapper,
                        ObjectMapper objectMapper,
                        R2dbcTransactionManager transactionManager,
                        RabbitTemplate rabbitTemplate,
                        @Value("${rabbitmq.exchange.order}") String orderExchange,
                        @Value("${rabbitmq.exchange.ecommerce}") String ecommerceExchange) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderMapper = orderMapper;
        this.objectMapper = objectMapper;
        this.transactionalOperator = TransactionalOperator.create(transactionManager);
        this.rabbitTemplate = rabbitTemplate;
        this.orderExchange = orderExchange;
        this.ecommerceExchange = ecommerceExchange;
    }

    // Test-only constructor
    OrderService(OrderRepository orderRepository,
                 OrderItemRepository orderItemRepository,
                 OrderMapper orderMapper,
                 ObjectMapper objectMapper,
                 TransactionalOperator transactionalOperator,
                 RabbitTemplate rabbitTemplate,
                 String orderExchange,
                 String ecommerceExchange) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderMapper = orderMapper;
        this.objectMapper = objectMapper;
        this.transactionalOperator = transactionalOperator;
        this.rabbitTemplate = rabbitTemplate;
        this.orderExchange = orderExchange;
        this.ecommerceExchange = ecommerceExchange;
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
                    existing.setStatus(orderDto.status().name());
                    existing.setTotalAmount(orderDto.totalAmount());
                    existing.setCustomerId(orderDto.customerId());
                    return orderRepository.save(existing);
                })
                .flatMap(saved -> {
                    OrderEvent event = OrderEvent.statusChanged(saved.getId(), mapToEventOrderStatus(OrderEvent.OrderStatus.valueOf(saved.getStatus())));
                    rabbitTemplate.convertAndSend(orderExchange, "order.updated", event);
                    log.info("Published OrderEvent.UPDATED to RabbitMQ for order: {}", saved.getId());
                    return Mono.just(saved);
                })
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
                            .flatMap(saved -> {
                                List<OrderEvent.OrderItem> eventItems = toEventItems(saved.getItems());
                                OrderEvent event = OrderEvent.cancelled(saved.getId(), saved.getCustomerId(),
                                        saved.getCustomerEmail(), eventItems);
                                try {
                                    String jsonPayload = objectMapper.writeValueAsString(event);
                                    rabbitTemplate.convertAndSend(orderExchange, "order.cancelled", jsonPayload);
                                    log.info("Published OrderEvent.CANCELLED to RabbitMQ for order: {}", saved.getId());
                                } catch (JsonProcessingException e) {
                                    log.error("Failed to serialize OrderEvent for order: {}", saved.getId(), e);
                                    throw new RuntimeException("Failed to serialize OrderEvent", e);
                                }
                                return Mono.just(saved);
                            })
                            .map(orderMapper::toDto);
                }));
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
                .flatMap(saved -> {
                    OrderEvent event = OrderEvent.statusChanged(saved.getId(), mapToEventOrderStatus(OrderEvent.OrderStatus.valueOf(saved.getStatus())));
                    rabbitTemplate.convertAndSend(orderExchange, "order.updated", event);
                    log.info("Published OrderEvent.UPDATED to RabbitMQ for order: {}", saved.getId());
                    return Mono.just(saved);
                })
                .map(orderMapper::toDto));
    }

    public Mono<Void> handleReservationExpiry(Long orderId) {
        log.info("Handling reservation expiry for order: {}", orderId);
        return transactionalOperator.transactional(orderRepository.findById(orderId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Order", orderId)))
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

                    Mono<Order> savedOrder = itemsToUpdate
                            .then(orderRepository.save(order))
                            .flatMap(saved -> {
                                List<OrderEvent.OrderItem> eventItems = toEventItems(saved.getItems());
                                OrderEvent event = OrderEvent.cancelled(saved.getId(), saved.getCustomerId(),
                                        saved.getCustomerEmail(), eventItems);
                                try {
                                    String jsonPayload = objectMapper.writeValueAsString(event);
                                    rabbitTemplate.convertAndSend(orderExchange, "order.cancelled", jsonPayload);
                                    log.info("Published OrderEvent.CANCELLED to RabbitMQ for order: {}", saved.getId());
                                } catch (JsonProcessingException e) {
                                    log.error("Failed to serialize OrderEvent for order: {}", saved.getId(), e);
                                    throw new RuntimeException("Failed to serialize OrderEvent", e);
                                }
                                return Mono.just(saved);
                            });
                    return savedOrder.then();
                }));
    }

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
                    publishOrderEvent(updatedEvent);
                    return Mono.empty();
                })
                .switchIfEmpty(Mono.empty())
                .<Void>map(v -> null)
                .as(transactionalOperator::transactional);
    }

    private Mono<Void> processRefundFallback(Long orderId, Exception ex) {
        log.error("Fallback triggered for processRefund order {}: {}", orderId, ex.getMessage());
        return Mono.error(new RuntimeException("Payment service unavailable, refund processing failed", ex));
    }

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
                    publishOrderEvent(event);
                    return Mono.empty();
                }));
    }

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
                    publishOrderEvent(event);
                    return Mono.empty();
                }));
    }

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
                    publishOrderEvent(event);
                    return Mono.empty();
                }));
    }

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
                    publishOrderEvent(event);
                    return Mono.empty();
                }));
    }

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
                    publishOrderEvent(event);
                    return Mono.empty();
                }));
    }

    public void publishOrderEvent(OrderEvent event) {
        String routingKey;
        switch (event.getEventType()) {
            case "CREATED":
                routingKey = "order.created";
                break;
            case "UPDATED":
                routingKey = "order.updated";
                break;
            case "CANCELLED":
                routingKey = "order.cancelled";
                break;
            case "CONFIRMED":
                routingKey = "order.confirmed";
                break;
            case "SHIPPED":
                routingKey = "order.shipped";
                break;
            case "DELIVERED":
                routingKey = "order.delivered";
                break;
            default:
                routingKey = "order.updated";
        }
        rabbitTemplate.convertAndSend(orderExchange, routingKey, event);
        rabbitTemplate.convertAndSend(ecommerceExchange, routingKey, event);
        log.info("Published OrderEvent {} to RabbitMQ for order: {}", event.getEventType(), event.getOrderId());
    }
}