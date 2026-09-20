package com.example.order.scheduler;

import com.example.common.event.OutboxEventPublisher;
import com.example.common.event.ReservationExpiredEvent;
import com.example.order.model.Order;
import com.example.order.model.OrderItem;
import com.example.order.repository.OrderItemRepository;
import com.example.order.repository.OrderRepository;
import com.example.order.service.OrderSagaOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@Component
@EnableScheduling
public class ReservationExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpiryScheduler.class);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderSagaOrchestrator sagaOrchestrator;
    private final OutboxEventPublisher outboxPublisher;

    public ReservationExpiryScheduler(OrderRepository orderRepository,
                                      OrderItemRepository orderItemRepository,
                                      OrderSagaOrchestrator sagaOrchestrator,
                                      OutboxEventPublisher outboxPublisher) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.sagaOrchestrator = sagaOrchestrator;
        this.outboxPublisher = outboxPublisher;
    }

    @Scheduled(fixedRate = 60000)
    public void checkExpiredReservations() {
        log.debug("Checking for expired reservations");
        LocalDateTime now = LocalDateTime.now();

        orderRepository.findByStatus("RESERVED")
                .flatMap(order -> orderItemRepository.findByOrderIdAndStatus(order.getId(), OrderItem.OrderItemStatus.RESERVED).collectList()
                        .flatMap(reservedItems -> {
                            List<OrderItem> expiredItems = reservedItems.stream()
                                    .filter(item -> item.getReservedAt() != null &&
                                            item.getReservedAt().plusMinutes(30).isBefore(now))
                                    .toList();

                            if (expiredItems.isEmpty()) {
                                return Mono.empty();
                            }

                            return Flux.fromIterable(expiredItems)
                                    .flatMap(item -> {
                                        ReservationExpiredEvent event = ReservationExpiredEvent.expired(
                                                item.getId(), item.getVariantId(), item.getQuantityOrdered(), now);
                                        return sagaOrchestrator.handleReservationExpired(event)
                                                .thenReturn(item);
                                    })
                                    .then();
                        }))
                .subscribe(
                        unused -> log.debug("Expired reservation check completed"),
                        error -> log.error("Error checking expired reservations", error)
                );
    }
}