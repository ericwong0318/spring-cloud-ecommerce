package com.example.order.repository;

import com.example.order.model.Shipment;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ShipmentRepository extends R2dbcRepository<Shipment, Long> {
    Flux<Shipment> findByOrderId(Long orderId);
    Flux<Shipment> findByOrderIdAndStatus(Long orderId, Shipment.ShipmentStatus status);
}
