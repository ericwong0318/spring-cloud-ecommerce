package com.example.order.controller;

import com.example.order.model.Order;
import com.example.order.repository.OrderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderRepository orderRepository;

    public OrderController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @GetMapping
    public Flux<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Order>> getOrderById(@PathVariable Long id) {
        return orderRepository.findById(id)
                .map(order -> ResponseEntity.ok(order))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/customer/{customerId}")
    public Flux<Order> getOrdersByCustomerId(@PathVariable String customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    @PostMapping
    public Mono<ResponseEntity<Order>> createOrder(@RequestBody Order order) {
        // Generate a temporary ID if not provided (in real app, this might come from business logic)
        if (order.getId() == null) {
            order.setId(System.currentTimeMillis()); // Simple approach for demo
        }
        return orderRepository.save(order)
                .map(savedOrder -> ResponseEntity
                        .created(URI.create("/api/orders/" + savedOrder.getId()))
                        .body(savedOrder));
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<Order>> updateOrder(@PathVariable Long id, @RequestBody Order orderDetails) {
        return orderRepository.findById(id)
                .flatMap(existingOrder -> {
                    existingOrder.setStatus(orderDetails.getStatus());
                    existingOrder.setTotalAmount(orderDetails.getTotalAmount());
                    existingOrder.setCustomerId(orderDetails.getCustomerId());
                    return orderRepository.save(existingOrder);
                })
                .map(updatedOrder -> ResponseEntity.ok(updatedOrder))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteOrder(@PathVariable Long id) {
        return orderRepository.findById(id)
                .flatMap(existingOrder ->
                        orderRepository.deleteById(existingOrder.getId())
                                .then(Mono.just(ResponseEntity.<Void>noContent().<Void>build()))
                )
                .switchIfEmpty(Mono.just(ResponseEntity.<Void>notFound().<Void>build()));
    }
}