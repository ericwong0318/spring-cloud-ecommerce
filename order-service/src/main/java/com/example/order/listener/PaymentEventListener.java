package com.example.order.listener;

import com.example.common.event.BaseEvent;
import com.example.common.event.IdempotentEventProcessor;
import com.example.common.event.OrderEvent;
import com.example.common.event.PaymentEvent;
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

@Component
public class PaymentEventListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final IdempotentEventProcessor idempotentEventProcessor;
    private final OrderService orderService;

    public PaymentEventListener(OrderRepository orderRepository,
                                OrderItemRepository orderItemRepository,
                                IdempotentEventProcessor idempotentEventProcessor,
                                OrderService orderService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.idempotentEventProcessor = idempotentEventProcessor;
        this.orderService = orderService;
    }

    @RabbitListener(queues = "${rabbitmq.queue.payment-events}")
    @Transactional
    public void handlePaymentEvent(PaymentEvent event) {
        idempotentEventProcessor.process(event, this::handlePaymentEventInternal);
    }

    private void handlePaymentEventInternal(PaymentEvent event) {
        log.info("Received payment event: eventType={}, eventId={}, orderId={}, status={}",
                event.getEventType(), event.getEventId(), event.getOrderId(), event.getStatus());

        if (event.getOrderId() == null) {
            log.warn("Payment event missing orderId, skipping: eventId={}", event.getEventId());
            return;
        }

        switch (PaymentEvent.EventType.valueOf(event.getEventType())) {
            case CAPTURED -> handlePaymentCaptured(event);
            case FAILED -> handlePaymentFailed(event);
            case REFUNDED -> {
                if (event.getStatus() == PaymentEvent.PaymentStatus.PARTIALLY_REFUNDED) {
                    handlePaymentPartiallyRefunded(event);
                } else {
                    handlePaymentRefunded(event);
                }
            }
            case AUTHORIZED -> log.info("Payment authorized for order: {}, no order state change needed", event.getOrderId());
            default -> log.debug("Unhandled payment event type: {}", event.getEventType());
        }
    }

    private void handlePaymentCaptured(PaymentEvent event) {
        log.info("Handling payment captured for order: {}", event.getOrderId());

        orderRepository.findById(event.getOrderId())
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Order", event.getOrderId())))
                .flatMap(order -> {
                    if (!"PENDING".equals(order.getStatus())) {
                        log.info("Order {} is not in PENDING state (current: {}), skipping confirmation", event.getOrderId(), order.getStatus());
                        return Mono.empty();
                    }

                    order.setStatus("CONFIRMED");
                    return orderRepository.save(order)
                            .flatMap(saved -> {
                                for (OrderItem item : saved.getItems()) {
                                    if (item.getStatus() == OrderItem.OrderItemStatus.PENDING) {
                                        item.setStatus(OrderItem.OrderItemStatus.RESERVED);
                                        orderItemRepository.save(item).subscribe();
                                    }
                                }
                                log.info("Order {} transitioned to CONFIRMED, items updated to RESERVED", event.getOrderId());

                                OrderEvent updatedEvent = OrderEvent.statusChanged(event.getOrderId(), OrderEvent.OrderStatus.CONFIRMED);
                                orderService.publishOrderEvent(updatedEvent);
                                return Mono.empty();
                            });
                })
                .subscribe();
    }

    private void handlePaymentFailed(PaymentEvent event) {
        log.info("Handling payment failed for order: {}", event.getOrderId());

        orderRepository.findById(event.getOrderId())
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Order", event.getOrderId())))
                .flatMap(order -> {
                    if (!"PENDING".equals(order.getStatus())) {
                        log.info("Order {} is not in PENDING state (current: {}), skipping cancellation", event.getOrderId(), order.getStatus());
                        return Mono.empty();
                    }

                    order.setStatus("CANCELLED");
                    return orderRepository.save(order)
                            .flatMap(saved -> {
                                for (OrderItem item : saved.getItems()) {
                                    if (item.getStatus() == OrderItem.OrderItemStatus.PENDING) {
                                        item.setStatus(OrderItem.OrderItemStatus.CANCELLED);
                                        orderItemRepository.save(item).subscribe();
                                    }
                                }
                                log.info("Order {} transitioned to CANCELLED, items updated to CANCELLED", event.getOrderId());

                                OrderEvent cancelledEvent = OrderEvent.cancelled(event.getOrderId(),
                                        saved.getCustomerId(), null, saved.getItems().stream()
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
                                return Mono.empty();
                            });
                })
                .subscribe();
    }

    private void handlePaymentRefunded(PaymentEvent event) {
        log.info("Handling payment refunded for order: {}", event.getOrderId());

        orderRepository.findById(event.getOrderId())
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Order", event.getOrderId())))
                .flatMap(order -> {
                    if (!"CONFIRMED".equals(order.getStatus()) && !"SHIPPED".equals(order.getStatus()) && !"DELIVERED".equals(order.getStatus())) {
                        log.info("Order {} is not in a refundable state (current: {}), skipping refund handling", event.getOrderId(), order.getStatus());
                        return Mono.empty();
                    }

                    for (OrderItem item : order.getItems()) {
                        if (item.getStatus() == OrderItem.OrderItemStatus.RESERVED || item.getStatus() == OrderItem.OrderItemStatus.SHIPPED) {
                            item.setStatus(OrderItem.OrderItemStatus.CANCELLED);
                            orderItemRepository.save(item).subscribe();
                        }
                    }

                    boolean allItemsCancelled = order.getItems().stream()
                            .allMatch(item -> item.getStatus() == OrderItem.OrderItemStatus.CANCELLED);

                    if (allItemsCancelled) {
                        order.setStatus("CANCELLED");
                        return orderRepository.save(order)
                                .doOnNext(saved -> {
                                    log.info("Order {} transitioned to CANCELLED after full refund", event.getOrderId());

                                    OrderEvent updatedEvent = OrderEvent.statusChanged(event.getOrderId(), OrderEvent.OrderStatus.valueOf(saved.getStatus()));
                                    orderService.publishOrderEvent(updatedEvent);
                                })
                                .then();
                    } else {
                        log.info("Order {} partially refunded, some items remain active", event.getOrderId());

                        OrderEvent updatedEvent = OrderEvent.statusChanged(event.getOrderId(), OrderEvent.OrderStatus.valueOf(order.getStatus()));
                        orderService.publishOrderEvent(updatedEvent);
                        return Mono.empty();
                    }
                })
                .subscribe();
    }

    private void handlePaymentPartiallyRefunded(PaymentEvent event) {
        log.info("Handling payment partially refunded for order: {}", event.getOrderId());

        orderRepository.findById(event.getOrderId())
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Order", event.getOrderId())))
                .flatMap(order -> {
                    if (!"CONFIRMED".equals(order.getStatus()) && !"SHIPPED".equals(order.getStatus()) && !"DELIVERED".equals(order.getStatus())) {
                        log.info("Order {} is not in a refundable state (current: {}), skipping partial refund handling", event.getOrderId(), order.getStatus());
                        return Mono.empty();
                    }

                    for (OrderItem item : order.getItems()) {
                        if (item.getStatus() == OrderItem.OrderItemStatus.RESERVED) {
                            item.setStatus(OrderItem.OrderItemStatus.CANCELLED);
                            orderItemRepository.save(item).subscribe();
                        }
                    }

                    log.info("Order {} partially refunded, reserved items cancelled", event.getOrderId());

                    OrderEvent updatedEvent = OrderEvent.statusChanged(event.getOrderId(), OrderEvent.OrderStatus.valueOf(order.getStatus()));
                    orderService.publishOrderEvent(updatedEvent);
                    return Mono.empty();
                })
                .subscribe();
    }
}