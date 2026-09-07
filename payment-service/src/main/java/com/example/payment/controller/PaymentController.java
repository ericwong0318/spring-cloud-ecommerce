package com.example.payment.controller;

import com.example.common.dto.PaymentDto;
import com.example.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.net.URI;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/authorize")
    public Mono<ResponseEntity<PaymentDto>> authorizePayment(
            @RequestParam Long orderId,
            @RequestParam BigDecimal amount,
            @RequestParam String currency,
            @RequestParam String customerId,
            @RequestParam String customerEmail) {
        return paymentService.authorizePayment(orderId, amount, currency, customerId, customerEmail)
                .map(authorized -> ResponseEntity
                        .created(URI.create("/api/payments/" + authorized.getId()))
                        .body(authorized));
    }

    @PostMapping("/{id}/capture")
    public Mono<ResponseEntity<PaymentDto>> capturePayment(
            @PathVariable Long id,
            @RequestParam String gatewayTransactionId) {
        return paymentService.capturePayment(id, gatewayTransactionId)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/{id}/refund")
    public Mono<ResponseEntity<PaymentDto>> refundPayment(
            @PathVariable Long id,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String reason) {
        return paymentService.refundPayment(id, amount, reason)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<PaymentDto>> getPaymentById(@PathVariable Long id) {
        return paymentService.getPaymentById(id)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/order/{orderId}")
    public Mono<ResponseEntity<PaymentDto>> getPaymentByOrderId(@PathVariable Long orderId) {
        return paymentService.getPaymentByOrderId(orderId)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/order/{orderId}/all")
    public reactor.core.publisher.Flux<PaymentDto> getPaymentsByOrderId(@PathVariable Long orderId) {
        return paymentService.getPaymentsByOrderId(orderId);
    }
}