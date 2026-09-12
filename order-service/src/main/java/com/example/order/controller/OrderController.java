package com.example.order.controller;

import com.example.common.dto.OrderDto;
import com.example.common.exception.ResourceNotFoundException;
import com.example.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public Mono<ResponseEntity<OrderDto>> createOrder(@Valid @RequestBody OrderDto orderDto) {
        return orderService.createOrder(orderDto)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<OrderDto>> getOrder(@PathVariable Long id) {
        return orderService.getOrderById(id)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> e instanceof ResourceNotFoundException,
                        e -> Mono.just(ResponseEntity.notFound().build()));
    }

    @PostMapping("/{id}/cancel")
    public Mono<ResponseEntity<OrderDto>> cancelOrder(@PathVariable Long id) {
        return orderService.cancelOrder(id)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> e instanceof IllegalStateException,
                        e -> Mono.just(ResponseEntity.status(409).build()))
                .onErrorResume(e -> e instanceof ResourceNotFoundException,
                        e -> Mono.just(ResponseEntity.notFound().build()));
    }
}
