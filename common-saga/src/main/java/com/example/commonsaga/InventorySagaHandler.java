package com.example.commonsaga;

import com.example.common.event.InventoryEvent;
import com.example.common.event.OrderEvent;
import reactor.core.publisher.Mono;

public interface InventorySagaHandler {

    Mono<Void> handleOrderCreated(OrderEvent event);

    Mono<Void> handleOrderCancelled(OrderEvent event);
}