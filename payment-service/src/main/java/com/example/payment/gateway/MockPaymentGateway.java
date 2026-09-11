package com.example.payment.gateway;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Random;

@Component
public class MockPaymentGateway {

    private static final String SUCCESS_HEADER = "X-Mock-Gateway-Success";
    private final Random random = new Random();

    public Mono<GatewayResponse> authorize(BigDecimal amount, String currency, String idempotencyKey, String successHeader) {
        boolean forceSuccess = "true".equalsIgnoreCase(successHeader);
        boolean forceFailure = "false".equalsIgnoreCase(successHeader);

        if (forceFailure) {
            return Mono.just(new GatewayResponse(false, "MOCK_DECLINED", "Mock gateway forced decline", null));
        }

        if (forceSuccess) {
            String transactionId = "mock_txn_" + System.currentTimeMillis() + "_" + random.nextInt(10000);
            return Mono.just(new GatewayResponse(true, "APPROVED", "Mock authorization successful", transactionId));
        }

        if (amount.compareTo(new BigDecimal("10000")) > 0) {
            return Mono.just(new GatewayResponse(false, "AMOUNT_TOO_HIGH", "Amount exceeds mock gateway limit", null));
        }

        String transactionId = "mock_txn_" + System.currentTimeMillis() + "_" + random.nextInt(10000);
        return Mono.just(new GatewayResponse(true, "APPROVED", "Mock authorization successful", transactionId));
    }

    public Mono<GatewayResponse> capture(String gatewayTransactionId, String successHeader) {
        boolean forceSuccess = "true".equalsIgnoreCase(successHeader);
        boolean forceFailure = "false".equalsIgnoreCase(successHeader);

        if (forceFailure) {
            return Mono.just(new GatewayResponse(false, "MOCK_CAPTURE_DECLINED", "Mock gateway forced capture decline", null));
        }

        return Mono.just(new GatewayResponse(true, "CAPTURED", "Mock capture successful", gatewayTransactionId));
    }

    public Mono<GatewayResponse> refund(String gatewayTransactionId, BigDecimal amount, String successHeader) {
        boolean forceSuccess = "true".equalsIgnoreCase(successHeader);
        boolean forceFailure = "false".equalsIgnoreCase(successHeader);

        if (forceFailure) {
            return Mono.just(new GatewayResponse(false, "MOCK_REFUND_DECLINED", "Mock gateway forced refund decline", null));
        }

        String refundId = "mock_refund_" + System.currentTimeMillis() + "_" + random.nextInt(10000);
        return Mono.just(new GatewayResponse(true, "REFUNDED", "Mock refund successful", refundId));
    }

    public record GatewayResponse(boolean success, String code, String message, String transactionId) {}
}
