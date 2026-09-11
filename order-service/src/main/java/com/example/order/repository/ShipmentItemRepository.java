package com.example.order.repository;

import com.example.order.model.ShipmentItem;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ShipmentItemRepository extends R2dbcRepository<ShipmentItem, Long> {
    Flux<ShipmentItem> findByShipmentId(Long shipmentId);
    Flux<ShipmentItem> findByOrderItemId(Long orderItemId);
}
