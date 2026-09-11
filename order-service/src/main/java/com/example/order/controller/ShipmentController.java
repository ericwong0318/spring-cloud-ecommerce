package com.example.order.controller;

import com.example.common.dto.ShipmentDto;
import com.example.common.exception.ResourceNotFoundException;
import com.example.order.service.ShipmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders/{orderId}/shipments")
public class ShipmentController {

    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @PostMapping
    public Mono<ResponseEntity<ShipmentDto>> createShipment(@PathVariable Long orderId, @RequestBody ShipmentDto shipmentDto) {
        return shipmentService.createShipment(orderId, shipmentDto)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> e instanceof ResourceNotFoundException,
                        e -> Mono.just(ResponseEntity.notFound().build()))
                .onErrorResume(e -> e instanceof IllegalArgumentException || e instanceof IllegalStateException,
                        e -> Mono.just(ResponseEntity.badRequest().build()));
    }

    @GetMapping
    public Mono<ResponseEntity<List<ShipmentDto>>> getShipments(@PathVariable Long orderId) {
        return shipmentService.getShipmentsByOrderId(orderId)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> e instanceof ResourceNotFoundException,
                        e -> Mono.just(ResponseEntity.notFound().build()));
    }
}
