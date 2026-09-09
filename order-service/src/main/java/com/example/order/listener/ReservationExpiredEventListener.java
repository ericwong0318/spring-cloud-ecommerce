package com.example.order.listener;

import com.example.common.event.BaseEvent;
import com.example.common.event.IdempotentEventProcessor;
import com.example.common.event.OrderEvent;
import com.example.common.event.ReservationExpiredEvent;
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

@Component
public class ReservationExpiredEventListener {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpiredEventListener.class);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final IdempotentEventProcessor idempotentEventProcessor;
    private final OrderService orderService;

    public ReservationExpiredEventListener(OrderRepository orderRepository,
                                           OrderItemRepository orderItemRepository,
                                           IdempotentEventProcessor idempotentEventProcessor,
                                           OrderService orderService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.idempotentEventProcessor = idempotentEventProcessor;
        this.orderService = orderService;
    }

    @RabbitListener(queues = "${rabbitmq.queue.reservation-expired}")
    @Transactional
    public void handleReservationExpiredEvent(ReservationExpiredEvent event) {
        idempotentEventProcessor.process(event, this::handleReservationExpiredEventInternal);
    }

    private void handleReservationExpiredEventInternal(ReservationExpiredEvent event) {
        log.info("Received ReservationExpiredEvent: eventId={}, orderItemId={}, variantId={}, quantityReleased={}",
                event.getEventId(), event.getOrderItemId(), event.getVariantId(), event.getQuantityReleased());

        if (event.getOrderItemId() == null) {
            log.warn("ReservationExpiredEvent missing orderItemId, skipping: eventId={}", event.getEventId());
            return;
        }

        OrderItem orderItem = orderItemRepository.findById(event.getOrderItemId())
                .orElse(null);

        if (orderItem == null) {
            log.warn("OrderItem not found for id: {}", event.getOrderItemId());
            return;
        }

        // Only process if the item is in RESERVED status (already reserved but expired)
        if (orderItem.getStatus() != OrderItem.OrderItemStatus.RESERVED) {
            log.info("OrderItem {} is not in RESERVED status (current: {}), skipping cancellation",
                    event.getOrderItemId(), orderItem.getStatus());
            return;
        }

        // Cancel the order item
        orderItem.setStatus(OrderItem.OrderItemStatus.CANCELLED);
        orderItemRepository.save(orderItem);
        log.info("OrderItem {} cancelled due to reservation expiry", orderItem.getId());

        // Check parent order
        Order order = orderItem.getOrder();
        boolean allItemsCancelledOrBackordered = order.getItems().stream()
                .allMatch(item -> item.getStatus() == OrderItem.OrderItemStatus.CANCELLED ||
                        item.getStatus() == OrderItem.OrderItemStatus.BACKORDERED);

        if (allItemsCancelledOrBackordered) {
            order.setStatus("CANCELLED");
            orderRepository.save(order);
            log.info("Order {} cancelled due to all items being CANCELLED or BACKORDERED", order.getId());

            // Publish OrderEvent.CANCELLED
            OrderEvent cancelledEvent = OrderEvent.cancelled(order.getId(), order.getCustomerId(),
                    null, order.getItems().stream()
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
                            .toList());
            orderService.publishOrderEvent(cancelledEvent);
        }
    }
}