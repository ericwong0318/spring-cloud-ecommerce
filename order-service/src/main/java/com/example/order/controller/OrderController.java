package com.example.order.controller;

import com.example.common.dto.ShipmentDto;
import com.example.common.dto.ShipmentItemDto;
import com.example.common.event.OrderEvent;
import com.example.common.exception.ResourceNotFoundException;
import com.example.order.mapper.ShipmentMapper;
import com.example.order.model.Shipment;
import com.example.order.model.ShipmentItem;
import com.example.order.repository.ShipmentRepository;
import com.example.order.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final ShipmentMapper shipmentMapper;
    private final ShipmentRepository shipmentRepository;

    public OrderController(OrderService orderService, ShipmentMapper shipmentMapper, ShipmentRepository shipmentRepository) {
        this.orderService = orderService;
        this.shipmentMapper = shipmentMapper;
        this.shipmentRepository = shipmentRepository;
    }

    @PostMapping("/orders/{orderId}/shipment")
    public ResponseEntity<ShipmentDto> createShipment(@PathVariable Long orderId,
                                                      @RequestBody com.example.common.dto.ShipmentDto shipmentDto) {
        ShipmentDto createdShipment = orderService.createShipment(orderId, shipmentDto);
        return ResponseEntity.ok(createdShipment);
    }

    @GetMapping("/orders/{orderId}/shipments")
    public List<ShipmentDto> getShipments(@PathVariable Long orderId) {
        return shipmentRepository.findByOrderId(orderId).stream()
                .map(shipmentMapper::toDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/orders/{orderId}")
    public List<ShipmentDto> getShipment(@PathVariable Long orderId) {
        return shipmentRepository.findByOrderId(orderId).stream()
                .map(shipmentMapper::toDto)
                .collect(Collectors.toList());
    }
}