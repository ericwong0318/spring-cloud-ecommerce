package com.example.order.service;

import com.example.common.dto.ShipmentDto;
import com.example.common.dto.ShipmentItemDto;
import com.example.common.event.OrderEvent;
import com.example.common.exception.ResourceNotFoundException;
import com.example.order.mapper.ShipmentMapper;
import com.example.order.model.Order;
import com.example.order.model.OrderItem;
import com.example.order.model.Shipment;
import com.example.order.model.ShipmentItem;
import com.example.order.repository.OrderItemRepository;
import com.example.order.repository.OrderRepository;
import com.example.order.repository.ShipmentItemRepository;
import com.example.order.repository.ShipmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ShipmentService {

    private static final Logger log = LoggerFactory.getLogger(ShipmentService.class);

    private final ShipmentRepository shipmentRepository;
    private final ShipmentItemRepository shipmentItemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ShipmentMapper shipmentMapper;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final String orderExchange;
    private final String ecommerceExchange;
    private final TransactionalOperator transactionalOperator;

    public ShipmentService(ShipmentRepository shipmentRepository,
                           ShipmentItemRepository shipmentItemRepository,
                           OrderRepository orderRepository,
                           OrderItemRepository orderItemRepository,
                           ShipmentMapper shipmentMapper,
                           RabbitTemplate rabbitTemplate,
                           ObjectMapper objectMapper,
                           R2dbcTransactionManager transactionManager,
                           @Value("${rabbitmq.exchange.order}") String orderExchange,
                           @Value("${rabbitmq.exchange.ecommerce}") String ecommerceExchange) {
        this.shipmentRepository = shipmentRepository;
        this.shipmentItemRepository = shipmentItemRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.shipmentMapper = shipmentMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.orderExchange = orderExchange;
        this.ecommerceExchange = ecommerceExchange;
        this.transactionalOperator = TransactionalOperator.create(transactionManager);
    }

    // Test-only constructor
    @Autowired
    ShipmentService(ShipmentRepository shipmentRepository,
                    ShipmentItemRepository shipmentItemRepository,
                    OrderRepository orderRepository,
                    OrderItemRepository orderItemRepository,
                    ShipmentMapper shipmentMapper,
                    RabbitTemplate rabbitTemplate,
                    ObjectMapper objectMapper,
                    TransactionalOperator transactionalOperator,
                    @Value("${rabbitmq.exchange.order}") String orderExchange,
                    @Value("${rabbitmq.exchange.ecommerce}") String ecommerceExchange) {
        this.shipmentRepository = shipmentRepository;
        this.shipmentItemRepository = shipmentItemRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.shipmentMapper = shipmentMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.orderExchange = orderExchange;
        this.ecommerceExchange = ecommerceExchange;
        this.transactionalOperator = transactionalOperator;
    }

    public Mono<ShipmentDto> createShipment(Long orderId, ShipmentDto shipmentDto) {
        log.info("Creating shipment for order: {}", orderId);
        return transactionalOperator.transactional(
                orderRepository.findById(orderId)
                        .switchIfEmpty(Mono.error(new ResourceNotFoundException("Order", orderId)))
                        .flatMap(order -> validateAndCreateShipment(order, shipmentDto))
                        .map(shipmentMapper::toDto));
    }

    private Mono<Shipment> validateAndCreateShipment(Order order, ShipmentDto shipmentDto) {
        return Flux.fromIterable(shipmentDto.items())
                .flatMap(itemDto -> orderItemRepository.findById(itemDto.orderItemId())
                        .switchIfEmpty(Mono.error(new ResourceNotFoundException("OrderItem", itemDto.orderItemId())))
                        .filter(item -> item.getOrderId().equals(order.getId()))
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("Order item " + itemDto.orderItemId() + " does not belong to order " + order.getId())))
                        .flatMap(item -> {
                            int remainingQty = item.getRemainingQuantity();
                            if (itemDto.quantity() > remainingQty) {
                                return Mono.error(new IllegalArgumentException(
                                        "Quantity " + itemDto.quantity() + " exceeds remaining quantity " + remainingQty + " for order item " + itemDto.orderItemId()));
                            }
                            return Mono.just(item);
                        }))
                .collectList()
                .flatMap(validatedItems -> createShipmentWithItems(order, shipmentDto, validatedItems));
    }

    private Mono<Shipment> createShipmentWithItems(Order order, ShipmentDto shipmentDto, List<OrderItem> validatedItems) {
        Shipment shipment = new Shipment();
        shipment.setOrderId(order.getId());
        shipment.setTrackingNumber(shipmentDto.trackingNumber());
        shipment.setCarrier(shipmentDto.carrier());
        shipment.setStatus(Shipment.ShipmentStatus.CREATED);
        shipment.setCreatedAt(LocalDateTime.now());
        shipment.setUpdatedAt(LocalDateTime.now());

        return shipmentRepository.save(shipment)
                .flatMap(savedShipment -> createShipmentItems(savedShipment, shipmentDto, validatedItems))
                .flatMap(savedShipment -> reloadShipmentWithItems(savedShipment))
                .flatMap(savedShipment -> checkAndTransitionOrderToShipped(order.getId())
                        .then(Mono.just(savedShipment)))
                .flatMap(savedShipment -> publishShippedEvent(order, savedShipment)
                        .then(Mono.just(savedShipment)));
    }

    private Mono<Shipment> createShipmentItems(Shipment shipment, ShipmentDto shipmentDto, List<OrderItem> validatedItems) {
        return Flux.fromIterable(shipmentDto.items())
                .flatMap(itemDto -> {
                    OrderItem orderItem = validatedItems.stream()
                            .filter(i -> i.getId().equals(itemDto.orderItemId()))
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("Order item not found after validation"));

                    ShipmentItem shipmentItem = new ShipmentItem();
                    shipmentItem.setShipmentId(shipment.getId());
                    shipmentItem.setOrderItemId(itemDto.orderItemId());
                    shipmentItem.setQuantity(itemDto.quantity());

                    orderItem.setQuantityShipped(orderItem.getQuantityShipped() + itemDto.quantity());
                    if (orderItem.isFullyShipped()) {
                        orderItem.setStatus(OrderItem.OrderItemStatus.SHIPPED);
                    }

                    return orderItemRepository.save(orderItem)
                            .then(shipmentItemRepository.save(shipmentItem));
                })
                .then(Mono.just(shipment));
    }

    private Mono<Shipment> reloadShipmentWithItems(Shipment shipment) {
        return shipmentItemRepository.findByShipmentId(shipment.getId())
                .collectList()
                .map(items -> {
                    shipment.setItems(items);
                    return shipment;
                });
    }

    public Mono<List<ShipmentDto>> getShipmentsByOrderId(Long orderId) {
        log.debug("Fetching shipments for order: {}", orderId);
        return shipmentRepository.findByOrderId(orderId)
                .flatMap(shipment -> shipmentItemRepository.findByShipmentId(shipment.getId())
                        .collectList()
                        .map(items -> {
                            shipment.setItems(items);
                            return shipment;
                        }))
                .collectList()
                .map(shipmentMapper::toDtoList);
    }

    private Mono<Void> checkAndTransitionOrderToShipped(Long orderId) {
        return orderRepository.findById(orderId)
                .flatMap(order -> Flux.fromIterable(order.getItems())
                        .all(item -> item.getStatus() == OrderItem.OrderItemStatus.SHIPPED ||
                                item.getStatus() == OrderItem.OrderItemStatus.CANCELLED ||
                                item.getStatus() == OrderItem.OrderItemStatus.BACKORDERED)
                        .flatMap(allShippedOrCancelled -> {
                            if (allShippedOrCancelled) {
                                order.setStatus(OrderEvent.OrderStatus.SHIPPED.name());
                                return orderRepository.save(order)
                                        .flatMap(saved -> {
                                            OrderEvent updatedEvent = OrderEvent.statusChanged(saved.getId(), OrderEvent.OrderStatus.SHIPPED);
                                            publishOrderEvent(updatedEvent);
                                            return Mono.empty();
                                        });
                            }
                            return Mono.empty();
                        }))
                .switchIfEmpty(Mono.empty())
                .<Void>map(v -> null);
    }

    private Mono<Void> publishShippedEvent(Order order, Shipment shipment) {
        log.info("Publishing OrderEvent.SHIPPED for order: {} shipment: {}", order.getId(), shipment.getId());
        List<OrderEvent.OrderItem> eventItems = order.getItems().stream()
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

        OrderEvent shippedEvent = OrderEvent.shipped(
                order.getId(),
                order.getCustomerId(),
                null, // customerEmail not stored in order
                order.getTotalAmount(),
                eventItems,
                shipment.getId(),
                shipment.getTrackingNumber(),
                shipment.getCarrier(),
                LocalDateTime.now()
        );
        publishOrderEvent(shippedEvent);
        return Mono.empty();
    }

    void publishOrderEvent(OrderEvent event) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(event);
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
            rabbitTemplate.convertAndSend(orderExchange, routingKey, jsonPayload);
            rabbitTemplate.convertAndSend(ecommerceExchange, routingKey, jsonPayload);
            log.info("Published OrderEvent {} to RabbitMQ for order: {}", event.getEventType(), event.getOrderId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize OrderEvent for order: {}", event.getOrderId(), e);
            throw new RuntimeException("Failed to serialize OrderEvent", e);
        }
    }

    private OrderEvent.OrderItemStatus mapToEventItemStatus(OrderItem.OrderItemStatus status) {
        return OrderEvent.OrderItemStatus.valueOf(status.name());
    }
}