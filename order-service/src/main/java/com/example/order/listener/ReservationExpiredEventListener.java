package com.example.order.listener;

import com.example.common.event.BaseEvent;
import com.example.common.event.OrderEvent;
import com.example.common.event.ReservationExpiredEvent;
import com.example.common.event.ReactiveIdempotentEventProcessor;
import com.example.common.exception.ResourceNotFoundException;
import com.example.order.model.Order;
import com.example.order.model.OrderItem;
import com.example.common.event.OutboxEventPublisher;
import com.example.order.repository.OrderItemRepository;
import com.example.order.repository.OrderRepository;
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
    private final ReactiveIdempotentEventProcessor idempotentEventProcessor;
    private final OutboxEventPublisher outboxPublisher;

    public ReservationExpiredEventListener(OrderRepository orderRepository,
                                           OrderItemRepository orderItemRepository,
                                           ReactiveIdempotentEventProcessor idempotentEventProcessor,
                                           OutboxEventPublisher outboxPublisher) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.idempotentEventProcessor = idempotentEventProcessor;
        this.outboxPublisher = outboxPublisher;
    }

    @RabbitListener(queues = "${rabbitmq.queue.reservation-expired}")
    public void handleReservationExpiredEvent(ReservationExpiredEvent event) {
        idempotentEventProcessor.process(event, this::handleReservationExpiredEventInternal)
                .subscribe(
                        unused -> log.debug("Successfully processed reservation expired event: eventId={}", event.getEventId()),
                        error -> log.error("Failed to process reservation expired event: eventId={}, error={}",
                                event.getEventId(), error.getMessage())
                );
    }

    private Mono<Void> handleReservationExpiredEventInternal(ReservationExpiredEvent event) {
        log.info("Received ReservationExpiredEvent: eventId={}, orderItemId={}, variantId={}, quantityReleased={}",
                event.getEventId(), event.getOrderItemId(), event.getVariantId(), event.getQuantityReleased());

        if (event.getOrderItemId() == null) {
            log.warn("ReservationExpiredEvent missing orderItemId, skipping: eventId={}", event.getEventId());
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

                                            // Publish OrderEvent.CANCELLED to outbox
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
                                                        outboxPublisher.saveEvent("Order", saved.getId().toString(),
                                                                "ORDER_CANCELLED", cancelledEvent).subscribe();
                                                    });
                                        })
                                        .then();
                            }
                            return Mono.empty();
                        }));
    }
}