package com.example.inventory.saga;

import com.example.common.event.OrderEvent;
import reactor.core.publisher.Mono;

public interface InventorySagaOrchestrator {

    Mono<Void> handleOrderCreated(OrderEvent event);

    Mono<Void> handleOrderCancelled(OrderEvent event);
}