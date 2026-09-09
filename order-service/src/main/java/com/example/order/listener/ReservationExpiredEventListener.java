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

import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

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

        orderItemRepository.findById(event.getOrderItemId())
                .switchIfEmpty(Mono.empty())
                .filter(item -> item.getStatus() == OrderItem.OrderItemStatus.RESERVED)
                .flatMap(item -> {
                    item.setStatus(OrderItem.OrderItemStatus.CANCELLED);
                    return orderItemRepository.save(item);
                })
                .flatMap(savedItem -> checkAndCancelOrderIfAllItemsCancelledOrBackordered(savedItem.getOrderId())
                        .thenReturn(savedItem))
                .doOnNext(item -> log.info("OrderItem {} cancelled due to reservation expiry", item.getId()))
                .subscribe();
    }

    private Mono<Void> checkAndCancelOrderIfAllItemsCancelledOrBackordered(Long orderId) {
        return orderRepository.findById(orderId)
                .flatMap(order -> orderItemRepository.findByOrderId(orderId).collectList()
                        .flatMap(items -> {
                            boolean allItemsCancelledOrBackordered = items.stream()
                                    .allMatch(item -> item.getStatus() == OrderItem.OrderItemStatus.CANCELLED ||
                                            item.getStatus() == OrderItem.OrderItemStatus.BACKORDERED);

                            if (allItemsCancelledOrBackordered) {
                                order.setStatus("CANCELLED");
                                return orderRepository.save(order)
                                        .doOnNext(saved -> {
                                            log.info("Order {} cancelled due to all items being CANCELLED or BACKORDERED", saved.getId());

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
                        }));
    }
}