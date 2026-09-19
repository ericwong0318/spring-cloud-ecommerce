package com.example.order.service;

import com.example.common.event.OrderEvent;
import com.example.common.event.OutboxEventPublisher;
import com.example.order.model.Order;
import com.example.order.model.OrderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import reactor.core.publisher.Mono;

@Service
public class OrderEventPublisherImpl implements OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisherImpl.class);

    private final OutboxEventPublisher outboxPublisher;

    public OrderEventPublisherImpl(OutboxEventPublisher outboxPublisher) {
        this.outboxPublisher = outboxPublisher;
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
    public Mono<Void> publishOrderCreated(Order order) {
        List<OrderEvent.OrderItem> eventItems = toEventItems(order.getItems());
        OrderEvent event = OrderEvent.created(order.getId(), order.getCustomerId(),
                order.getCustomerEmail(), order.getTotalAmount(), eventItems);
        return outboxPublisher.saveEvent("Order", order.getId().toString(), "ORDER_CREATED", event);
    }

    @Override
    public Mono<Void> publishOrderUpdated(Order order) {
        OrderEvent event = OrderEvent.statusChanged(order.getId(), OrderEvent.OrderStatus.valueOf(order.getStatus()));
        return outboxPublisher.saveEvent("Order", order.getId().toString(), "ORDER_UPDATED", event);
    }

    @Override
    public Mono<Void> publishOrderCancelled(Order order) {
        List<OrderEvent.OrderItem> eventItems = toEventItems(order.getItems());
        OrderEvent event = OrderEvent.cancelled(order.getId(), order.getCustomerId(),
                order.getCustomerEmail(), eventItems);
        return outboxPublisher.saveEvent("Order", order.getId().toString(), "ORDER_CANCELLED", event);
    }

    @Override
    public Mono<Void> publishOrderConfirmed(Order order, BigDecimal amount) {
        List<OrderEvent.OrderItem> eventItems = toEventItems(order.getItems());
        OrderEvent event = OrderEvent.confirmed(order.getId(), order.getCustomerId(),
                order.getCustomerEmail(), amount, eventItems);
        return outboxPublisher.saveEvent("Order", order.getId().toString(), "ORDER_CONFIRMED", event);
    }

    @Override
    public Mono<Void> publishPaymentAuthorized(Order order) {
        OrderEvent event = OrderEvent.statusChanged(order.getId(), OrderEvent.OrderStatus.RESERVED);
        return outboxPublisher.saveEvent("Order", order.getId().toString(), "PAYMENT_AUTHORIZED", event);
    }

    @Override
    public Mono<Void> publishPaymentRefunded(Order order) {
        List<OrderEvent.OrderItem> eventItems = toEventItems(order.getItems());
        OrderEvent event = OrderEvent.refunded(order.getId(), order.getCustomerId(),
                order.getCustomerEmail(), order.getTotalAmount(), eventItems);
        return outboxPublisher.saveEvent("Order", order.getId().toString(), "PAYMENT_REFUNDED", event);
    }

    @Override
    public Mono<Void> publishPaymentPartiallyRefunded(Order order) {
        List<OrderEvent.OrderItem> eventItems = toEventItems(order.getItems());
        OrderEvent event = OrderEvent.partiallyRefunded(order.getId(), order.getCustomerId(),
                order.getCustomerEmail(), order.getTotalAmount(), eventItems);
        return outboxPublisher.saveEvent("Order", order.getId().toString(), "PAYMENT_PARTIALLY_REFUNDED", event);
    }

    @Override
    public Mono<Void> publishOrderEvent(OrderEvent event) {
        String eventType = event.getEventType();
        String outboxEventType = switch (eventType) {
            case "CREATED" -> "ORDER_CREATED";
            case "UPDATED" -> "ORDER_UPDATED";
            case "CANCELLED" -> "ORDER_CANCELLED";
            case "CONFIRMED" -> "ORDER_CONFIRMED";
            case "SHIPPED" -> "ORDER_SHIPPED";
            case "DELIVERED" -> "ORDER_DELIVERED";
            default -> "ORDER_UPDATED";
        };
        return outboxPublisher.saveEvent("Order", event.getOrderId().toString(), outboxEventType, event);
    }
}