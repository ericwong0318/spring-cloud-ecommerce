package com.example.payment.controller;

import com.example.common.dto.AuthorizeRequest;
import com.example.common.dto.CaptureRequest;
import com.example.common.dto.PaymentDto;
import com.example.common.dto.RefundRequest;
import com.example.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.net.URI;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/authorize")
    public Mono<ResponseEntity<PaymentDto>> authorizePayment(
            @Valid @RequestBody AuthorizeRequest request) {
        return paymentService.authorizePayment(request.orderId(), request.amount(),
                request.currency(), request.customerId(), request.customerEmail(),
                request.idempotencyKey())
                .map(authorized -> ResponseEntity
                        .created(URI.create("/payments/" + authorized.id()))
                        .body(authorized));
    }

    @PostMapping("/{id}/capture")
    public Mono<ResponseEntity<PaymentDto>> capturePayment(
            @PathVariable Long id,
            @Valid @RequestBody CaptureRequest request) {
        return paymentService.capturePayment(id, request.gatewayTransactionId(),
                "capture-" + id + "-" + request.gatewayTransactionId())
                .map(ResponseEntity::ok);
    }

    @PostMapping("/{id}/refund")
    public Mono<ResponseEntity<PaymentDto>> refundPayment(
            @PathVariable Long id,
            @Valid @RequestBody RefundRequest request) {
        return paymentService.refundPayment(id, request.amount(), request.reason(),
                "refund-" + id + "-" + request.amount())
                .map(ResponseEntity::ok);
    }

    @PostMapping("/webhook/{gateway}")
    public Mono<ResponseEntity<Void>> handleWebhook(
            @PathVariable String gateway,
            @RequestBody String payload,
            @RequestHeader("X-Signature") String signature) {
        log.info("Received webhook from {}: {}", gateway, payload);
        // TODO: Verify signature, parse event, update payment status
        return Mono.just(ResponseEntity.ok().build());
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

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PaymentController.class);
}
