package com.example.order.listener;

import com.example.common.event.BaseEvent;
import com.example.common.event.IdempotentEventProcessor;
import com.example.common.event.InventoryEvent;
import com.example.common.event.OrderEvent;
import com.example.common.exception.ResourceNotFoundException;
import com.example.order.model.Order;
import com.example.order.model.OrderItem;
import com.example.order.repository.OrderItemRepository;
import com.example.order.repository.OrderRepository;
import com.example.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Component
public class InventoryEventListener {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventListener.class);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final IdempotentEventProcessor idempotentEventProcessor;
    private final OrderService orderService;

    public InventoryEventListener(OrderRepository orderRepository,
                                  OrderItemRepository orderItemRepository,
                                  IdempotentEventProcessor idempotentEventProcessor,
                                  OrderService orderService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.idempotentEventProcessor = idempotentEventProcessor;
        this.orderService = orderService;
    }

    @RabbitListener(queues = "${rabbitmq.queue.inventory-events}")
    @Transactional
    public void handleInventoryEvent(InventoryEvent event) {
        idempotentEventProcessor.process(event, this::handleInventoryEventInternal);
    }

    private void handleInventoryEventInternal(InventoryEvent event) {
        log.info("Received inventory event: eventType={}, eventId={}, variantId={}, reserved={}, backordered={}",
                event.getEventType(), event.getEventId(), event.getVariantId(), event.getReserved(), event.getBackordered());

        if (event.getVariantId() == null) {
            log.warn("Inventory event missing variantId, skipping: eventId={}", event.getEventId());
            return;
        }

        switch (InventoryEvent.EventType.valueOf(event.getEventType())) {
            case RESERVED -> handleStockReserved(event);
            case RELEASED -> handleStockReleased(event);
            case CONFIRMED -> handleStockConfirmed(event);
            default -> log.debug("Unhandled inventory event type: {}", event.getEventType());
        }
    }

    private void handleStockReserved(InventoryEvent event) {
        int reserved = event.getReserved() != null ? event.getReserved() : 0;
        int backordered = event.getBackordered() != null ? event.getBackordered() : 0;

        if (reserved > 0) {
            log.info("Stock reserved for variant: {}, reserved={}, backordered={}", event.getVariantId(), reserved, backordered);
            updateOrderItemStatusForReserved(event.getVariantId(), reserved, backordered);
        } else if (backordered > 0) {
            log.warn("Stock reservation failed for variant: {}, fully backordered={}", event.getVariantId(), backordered);
            handleStockReservationFailed(event.getVariantId(), backordered);
        }
    }

    private void updateOrderItemStatusForReserved(Long variantId, int reserved, int backordered) {
        // Find the order item by variantId that is in PENDING status
        OrderItem orderItem = orderItemRepository.findByVariantIdAndStatus(variantId, OrderItem.OrderItemStatus.PENDING)
                .block(); // Block to get the result

        if (orderItem == null) {
            log.warn("No PENDING OrderItem found for variantId: {}", variantId);
            return;
        }

        // Update the order item status
        if (backordered > 0) {
            orderItem.setStatus(OrderItem.OrderItemStatus.BACKORDERED);
            log.info("OrderItem {} set to BACKORDERED (reserved={}, backordered={})", orderItem.getId(), reserved, backordered);
        } else {
            orderItem.setStatus(OrderItem.OrderItemStatus.RESERVED);
            orderItem.setReservedAt(java.time.LocalDateTime.now());
            log.info("OrderItem {} set to RESERVED", orderItem.getId());
        }
        orderItemRepository.save(orderItem).block();

        // Check if all order items are RESERVED or BACKORDERED
        Long orderId = orderItem.getOrderId();
        orderRepository.findById(orderId)
                .flatMap(order -> {
                    // Fetch all items for this order
                    return orderItemRepository.findByOrderId(orderId).collectList()
                            .flatMap(items -> {
                                boolean allItemsReservedOrBackordered = items.stream()
                                        .allMatch(item -> item.getStatus() == OrderItem.OrderItemStatus.RESERVED ||
                                                item.getStatus() == OrderItem.OrderItemStatus.BACKORDERED ||
                                                item.getStatus() == OrderItem.OrderItemStatus.SHIPPED);

                                if (allItemsReservedOrBackordered) {
                                    order.setStatus("RESERVED");
                                    return orderRepository.save(order)
                                            .doOnNext(saved -> {
                                                log.info("Order {} transitioned to RESERVED", saved.getId());

                                                // Publish OrderEvent.UPDATED
                                                OrderEvent updatedEvent = OrderEvent.statusChanged(saved.getId(), OrderEvent.OrderStatus.valueOf("RESERVED"));
                                                orderService.publishOrderEvent(updatedEvent);
                                            })
                                            .then();
                                }
                                return Mono.empty();
                            });
                })
                .subscribe();
    }

    private void handleStockReservationFailed(Long variantId, int backordered) {
        // Find the order item by variantId that is in PENDING status
        OrderItem orderItem = orderItemRepository.findByVariantIdAndStatus(variantId, OrderItem.OrderItemStatus.PENDING)
                .block();

        if (orderItem == null) {
            log.warn("No PENDING OrderItem found for variantId: {}", variantId);
            return;
        }

        Long orderId = orderItem.getOrderId();

        // Cancel the order item
        orderItem.setStatus(OrderItem.OrderItemStatus.CANCELLED);
        orderItemRepository.save(orderItem).block();
        log.info("OrderItem {} cancelled due to stock reservation failure", orderItem.getId());

        // Check if all items are now CANCELLED or BACKORDERED - if so, cancel the order
        orderRepository.findById(orderId)
                .flatMap(order -> orderItemRepository.findByOrderId(orderId).collectList()
                        .flatMap(orderItems -> {
                            boolean allItemsCancelledOrBackordered = orderItems.stream()
                                    .allMatch(item -> item.getStatus() == OrderItem.OrderItemStatus.CANCELLED ||
                                            item.getStatus() == OrderItem.OrderItemStatus.BACKORDERED);

                            if (allItemsCancelledOrBackordered) {
                                order.setStatus("CANCELLED");
                                return orderRepository.save(order)
                                        .doOnNext(saved -> {
                                            log.info("Order {} cancelled due to stock reservation failure", saved.getId());

                                            // Publish OrderEvent.CANCELLED
                                            orderItemRepository.findByOrderId(orderId).collectList()
                                                    .subscribe(cancelledItems -> {
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
                                                        orderService.publishOrderEvent(cancelledEvent);
                                                    });
                                        })
                                        .then();
                            }
                            return Mono.empty();
                        }))
                .subscribe();
    }

    private void handleStockReleased(InventoryEvent event) {
        log.info("Stock released for variant: {}", event.getVariantId());
        // Stock released due to order cancellation - order items already handled by PaymentEventListener
    }

    private void handleStockConfirmed(InventoryEvent event) {
        log.info("Stock confirmed for variant: {}", event.getVariantId());
        // Stock confirmed (payment captured) - order already transitioned to CONFIRMED by PaymentEventListener
    }
}