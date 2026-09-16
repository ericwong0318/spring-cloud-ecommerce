package com.example.payment;

import com.example.common.dto.AuthorizeRequest;
import com.example.common.dto.CaptureRequest;
import com.example.common.dto.PaymentDto;
import com.example.common.dto.RefundRequest;
import com.example.payment.repository.PaymentRepository;
import com.example.payment.repository.ProcessedEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
public class PaymentServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Test
    void testAuthorizePayment() {
        String idempotencyKey = "auth-test-" + UUID.randomUUID().toString().substring(0, 8);

        AuthorizeRequest request = new AuthorizeRequest(
                1L,
                new BigDecimal("1999.98"),
                "USD",
                "CUST-001",
                "customer@example.com",
                idempotencyKey
        );

        webTestClient.post()
                .uri("/payments/authorize")
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(PaymentDto.class)
                .value(response -> {
                    assertEquals(1L, response.orderId());
                    assertEquals(new BigDecimal("1999.98"), response.amount());
                    assertEquals("USD", response.currency());
                    assertEquals(PaymentDto.PaymentStatus.AUTHORIZED, response.status());
                    assertEquals(idempotencyKey, response.idempotencyKey());
                    assertNotNull(response.authorizedAt());
                });
    }

    @Test
    void testAuthorizePaymentIdempotency() {
        String idempotencyKey = "auth-idempotent-" + UUID.randomUUID().toString().substring(0, 8);

        AuthorizeRequest request = new AuthorizeRequest(
                2L,
                new BigDecimal("999.99"),
                "USD",
                "CUST-002",
                "customer2@example.com",
                idempotencyKey
        );

        webTestClient.post()
                .uri("/payments/authorize")
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated();

        webTestClient.post()
                .uri("/payments/authorize")
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void testCapturePayment() {
        String idempotencyKey = "auth-capture-" + UUID.randomUUID().toString().substring(0, 8);

        AuthorizeRequest authRequest = new AuthorizeRequest(
                3L,
                new BigDecimal("1499.99"),
                "USD",
                "CUST-003",
                "customer3@example.com",
                idempotencyKey
        );

        PaymentDto authResponse = webTestClient.post()
                .uri("/payments/authorize")
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(authRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(PaymentDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(authResponse);
        Long paymentId = authResponse.id();

        CaptureRequest captureRequest = new CaptureRequest("txn_123456");

        webTestClient.post()
                .uri("/payments/{id}/capture", paymentId)
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(captureRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(PaymentDto.class)
                .value(response -> {
                    assertEquals(paymentId, response.id());
                    assertEquals(PaymentDto.PaymentStatus.CAPTURED, response.status());
                    assertEquals("txn_123456", response.gatewayTransactionId());
                    assertNotNull(response.capturedAt());
                });
    }

    @Test
    void testCapturePaymentIdempotency() {
        String idempotencyKey = "auth-capture-idem-" + UUID.randomUUID().toString().substring(0, 8);

        AuthorizeRequest authRequest = new AuthorizeRequest(
                4L,
                new BigDecimal("2999.99"),
                "USD",
                "CUST-004",
                "customer4@example.com",
                idempotencyKey
        );

        PaymentDto authResponse = webTestClient.post()
                .uri("/payments/authorize")
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(authRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(PaymentDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(authResponse);
        Long paymentId = authResponse.id();

        CaptureRequest captureRequest = new CaptureRequest("txn_789012");

        webTestClient.post()
                .uri("/payments/{id}/capture", paymentId)
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(captureRequest)
                .exchange()
                .expectStatus().isOk();

        webTestClient.post()
                .uri("/payments/{id}/capture", paymentId)
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(captureRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(PaymentDto.class)
                .value(response -> {
                    assertEquals(paymentId, response.id());
                });
    }

    @Test
    void testRefundPaymentFull() {
        String idempotencyKey = "auth-refund-full-" + UUID.randomUUID().toString().substring(0, 8);

        AuthorizeRequest authRequest = new AuthorizeRequest(
                5L,
                new BigDecimal("1999.99"),
                "USD",
                "CUST-005",
                "customer5@example.com",
                idempotencyKey
        );

        PaymentDto authResponse = webTestClient.post()
                .uri("/payments/authorize")
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(authRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(PaymentDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(authResponse);
        Long paymentId = authResponse.id();

        CaptureRequest captureRequest = new CaptureRequest("txn_refund_1");

        webTestClient.post()
                .uri("/payments/{id}/capture", paymentId)
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(captureRequest)
                .exchange()
                .expectStatus().isOk();

        RefundRequest refundRequest = new RefundRequest(
                new BigDecimal("1999.99"),
                "Customer requested full refund"
        );

        webTestClient.post()
                .uri("/payments/{id}/refund", paymentId)
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(refundRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(PaymentDto.class)
                .value(response -> {
                    assertEquals(PaymentDto.PaymentStatus.REFUNDED, response.status());
                    assertNotNull(response.refundedAt());
                });
    }

    @Test
    void testRefundPaymentPartial() {
        String idempotencyKey = "auth-refund-partial-" + UUID.randomUUID().toString().substring(0, 8);

        AuthorizeRequest authRequest = new AuthorizeRequest(
                6L,
                new BigDecimal("1000.00"),
                "USD",
                "CUST-006",
                "customer6@example.com",
                idempotencyKey
        );

        PaymentDto authResponse = webTestClient.post()
                .uri("/payments/authorize")
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(authRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(PaymentDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(authResponse);
        Long paymentId = authResponse.id();

        CaptureRequest captureRequest = new CaptureRequest("txn_refund_2");

        webTestClient.post()
                .uri("/payments/{id}/capture", paymentId)
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(captureRequest)
                .exchange()
                .expectStatus().isOk();

        RefundRequest refundRequest = new RefundRequest(
                new BigDecimal("500.00"),
                "Partial refund for returned item"
        );

        webTestClient.post()
                .uri("/payments/{id}/refund", paymentId)
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(refundRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(PaymentDto.class)
                .value(response -> {
                    assertEquals(PaymentDto.PaymentStatus.PARTIALLY_REFUNDED, response.status());
                    assertNotNull(response.refundedAt());
                });
    }

    @Test
    void testGetPaymentById() {
        String idempotencyKey = "auth-get-" + UUID.randomUUID().toString().substring(0, 8);

        AuthorizeRequest authRequest = new AuthorizeRequest(
                7L,
                new BigDecimal("750.00"),
                "USD",
                "CUST-007",
                "customer7@example.com",
                idempotencyKey
        );

        PaymentDto authResponse = webTestClient.post()
                .uri("/payments/authorize")
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(authRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(PaymentDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(authResponse);
        Long paymentId = authResponse.id();

        webTestClient.get()
                .uri("/payments/{id}", paymentId)
                .header("Authorization", "Bearer test-token")
                .exchange()
                .expectStatus().isOk()
                .expectBody(PaymentDto.class)
                .value(response -> {
                    assertEquals(paymentId, response.id());
                });
    }

    @Test
    void testGetPaymentByOrderId() {
        String idempotencyKey = "auth-get-order-" + UUID.randomUUID().toString().substring(0, 8);

        AuthorizeRequest authRequest = new AuthorizeRequest(
                8L,
                new BigDecimal("1250.00"),
                "USD",
                "CUST-008",
                "customer8@example.com",
                idempotencyKey
        );

        PaymentDto authResponse = webTestClient.post()
                .uri("/payments/authorize")
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(authRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(PaymentDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(authResponse);
        Long paymentId = authResponse.id();

        webTestClient.get()
                .uri("/payments/order/{orderId}", 8L)
                .header("Authorization", "Bearer test-token")
                .exchange()
                .expectStatus().isOk()
                .expectBody(PaymentDto.class)
                .value(response -> {
                    assertEquals(paymentId, response.id());
                });
    }
}