package com.example.payment.controller;

import com.example.common.dto.PaymentDto;
import com.example.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/authorize")
    public ResponseEntity<PaymentDto> authorizePayment(
            @RequestParam Long orderId,
            @RequestParam BigDecimal amount,
            @RequestParam String currency,
            @RequestParam String customerId,
            @RequestParam String customerEmail) {
        PaymentDto authorized = paymentService.authorizePayment(orderId, amount, currency, customerId, customerEmail);
        return ResponseEntity
                .created(URI.create("/api/payments/" + authorized.getId()))
                .body(authorized);
    }

    @PostMapping("/{id}/capture")
    public ResponseEntity<PaymentDto> capturePayment(
            @PathVariable Long id,
            @RequestParam String gatewayTransactionId) {
        return ResponseEntity.ok(paymentService.capturePayment(id, gatewayTransactionId));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<PaymentDto> refundPayment(
            @PathVariable Long id,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(paymentService.refundPayment(id, amount, reason));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentDto> getPaymentById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentDto> getPaymentByOrderId(@PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.getPaymentByOrderId(orderId));
    }
}