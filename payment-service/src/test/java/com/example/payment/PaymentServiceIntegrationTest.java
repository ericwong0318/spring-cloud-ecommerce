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

        AuthorizeRequest request = AuthorizeRequest.builder()
                .orderId(1L)
                .amount(new BigDecimal("1999.98"))
                .currency("USD")
                .customerId("CUST-001")
                .customerEmail("customer@example.com")
                .idempotencyKey(idempotencyKey)
                .build();

        webTestClient.post()
                .uri("/api/v1/payments/authorize")
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(PaymentDto.class)
                .value(response -> {
                    assertEquals(1L, response.getOrderId());
                    assertEquals(new BigDecimal("1999.98"), response.getAmount());
                    assertEquals("USD", response.getCurrency());
                    assertEquals(PaymentDto.PaymentStatus.AUTHORIZED, response.getStatus());
                    assertEquals(idempotencyKey, response.getIdempotencyKey());
                    assertNotNull(response.getAuthorizedAt());
                });
    }

    @Test
    void testAuthorizePaymentIdempotency() {
        String idempotencyKey = "auth-idempotent-" + UUID.randomUUID().toString().substring(0, 8);

        AuthorizeRequest request = AuthorizeRequest.builder()
                .orderId(2L)
                .amount(new BigDecimal("999.99"))
                .currency("USD")
                .customerId("CUST-002")
                .customerEmail("customer2@example.com")
                .idempotencyKey(idempotencyKey)
                .build();

        webTestClient.post()
                .uri("/api/v1/payments/authorize")
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated();

        webTestClient.post()
                .uri("/api/v1/payments/authorize")
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void testCapturePayment() {
        String idempotencyKey = "auth-capture-" + UUID.randomUUID().toString().substring(0, 8);

        AuthorizeRequest authRequest = AuthorizeRequest.builder()
                .orderId(3L)
                .amount(new BigDecimal("1499.99"))
                .currency("USD")
                .customerId("CUST-003")
                .customerEmail("customer3@example.com")
                .idempotencyKey(idempotencyKey)
                .build();

        PaymentDto authResponse = webTestClient.post()
                .uri("/api/v1/payments/authorize")
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(authRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(PaymentDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(authResponse);
        Long paymentId = authResponse.getId();

        CaptureRequest captureRequest = CaptureRequest.builder()
                .gatewayTransactionId("txn_123456")
                .build();

        webTestClient.post()
                .uri("/api/v1/payments/{id}/capture", paymentId)
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(captureRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(PaymentDto.class)
                .value(response -> {
                    assertEquals(paymentId, response.getId());
                    assertEquals(PaymentDto.PaymentStatus.CAPTURED, response.getStatus());
                    assertEquals("txn_123456", response.getGatewayTransactionId());
                    assertNotNull(response.getCapturedAt());
                });
    }

    @Test
    void testCapturePaymentIdempotency() {
        String idempotencyKey = "auth-capture-idem-" + UUID.randomUUID().toString().substring(0, 8);

        AuthorizeRequest authRequest = AuthorizeRequest.builder()
                .orderId(4L)
                .amount(new BigDecimal("2999.99"))
                .currency("USD")
                .customerId("CUST-004")
                .customerEmail("customer4@example.com")
                .idempotencyKey(idempotencyKey)
                .build();

        PaymentDto authResponse = webTestClient.post()
                .uri("/api/v1/payments/authorize")
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(authRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(PaymentDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(authResponse);
        Long paymentId = authResponse.getId();

        CaptureRequest captureRequest = CaptureRequest.builder()
                .gatewayTransactionId("txn_789012")
                .build();

        webTestClient.post()
                .uri("/api/v1/payments/{id}/capture", paymentId)
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(captureRequest)
                .exchange()
                .expectStatus().isOk();

        webTestClient.post()
                .uri("/api/v1/payments/{id}/capture", paymentId)
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(captureRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(PaymentDto.class)
                .value(response -> {
                    assertEquals(paymentId, response.getId());
                });
    }

    @Test
    void testRefundPaymentFull() {
        String idempotencyKey = "auth-refund-full-" + UUID.randomUUID().toString().substring(0, 8);

        AuthorizeRequest authRequest = AuthorizeRequest.builder()
                .orderId(5L)
                .amount(new BigDecimal("1999.99"))
                .currency("USD")
                .customerId("CUST-005")
                .customerEmail("customer5@example.com")
                .idempotencyKey(idempotencyKey)
                .build();

        PaymentDto authResponse = webTestClient.post()
                .uri("/api/v1/payments/authorize")
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(authRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(PaymentDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(authResponse);
        Long paymentId = authResponse.getId();

        CaptureRequest captureRequest = CaptureRequest.builder()
                .gatewayTransactionId("txn_refund_1")
                .build();

        webTestClient.post()
                .uri("/api/v1/payments/{id}/capture", paymentId)
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(captureRequest)
                .exchange()
                .expectStatus().isOk();

        RefundRequest refundRequest = RefundRequest.builder()
                .amount(new BigDecimal("1999.99"))
                .reason("Customer requested full refund")
                .build();

        webTestClient.post()
                .uri("/api/v1/payments/{id}/refund", paymentId)
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(refundRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(PaymentDto.class)
                .value(response -> {
                    assertEquals(PaymentDto.PaymentStatus.REFUNDED, response.getStatus());
                    assertNotNull(response.getRefundedAt());
                });
    }

    @Test
    void testRefundPaymentPartial() {
        String idempotencyKey = "auth-refund-partial-" + UUID.randomUUID().toString().substring(0, 8);

        AuthorizeRequest authRequest = AuthorizeRequest.builder()
                .orderId(6L)
                .amount(new BigDecimal("1000.00"))
                .currency("USD")
                .customerId("CUST-006")
                .customerEmail("customer6@example.com")
                .idempotencyKey(idempotencyKey)
                .build();

        PaymentDto authResponse = webTestClient.post()
                .uri("/api/v1/payments/authorize")
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(authRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(PaymentDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(authResponse);
        Long paymentId = authResponse.getId();

        CaptureRequest captureRequest = CaptureRequest.builder()
                .gatewayTransactionId("txn_refund_2")
                .build();

        webTestClient.post()
                .uri("/api/v1/payments/{id}/capture", paymentId)
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(captureRequest)
                .exchange()
                .expectStatus().isOk();

        RefundRequest refundRequest = RefundRequest.builder()
                .amount(new BigDecimal("500.00"))
                .reason("Partial refund for returned item")
                .build();

        webTestClient.post()
                .uri("/api/v1/payments/{id}/refund", paymentId)
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(refundRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(PaymentDto.class)
                .value(response -> {
                    assertEquals(PaymentDto.PaymentStatus.PARTIALLY_REFUNDED, response.getStatus());
                    assertNotNull(response.getRefundedAt());
                });
    }

    @Test
    void testGetPaymentById() {
        String idempotencyKey = "auth-get-" + UUID.randomUUID().toString().substring(0, 8);

        AuthorizeRequest authRequest = AuthorizeRequest.builder()
                .orderId(7L)
                .amount(new BigDecimal("750.00"))
                .currency("USD")
                .customerId("CUST-007")
                .customerEmail("customer7@example.com")
                .idempotencyKey(idempotencyKey)
                .build();

        PaymentDto authResponse = webTestClient.post()
                .uri("/api/v1/payments/authorize")
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(authRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(PaymentDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(authResponse);
        Long paymentId = authResponse.getId();

        webTestClient.get()
                .uri("/api/v1/payments/{id}", paymentId)
                .header("Authorization", "Bearer test-token")
                .exchange()
                .expectStatus().isOk()
                .expectBody(PaymentDto.class)
                .value(response -> {
                    assertEquals(paymentId, response.getId());
                });
    }

    @Test
    void testGetPaymentByOrderId() {
        String idempotencyKey = "auth-get-order-" + UUID.randomUUID().toString().substring(0, 8);

        AuthorizeRequest authRequest = AuthorizeRequest.builder()
                .orderId(8L)
                .amount(new BigDecimal("1250.00"))
                .currency("USD")
                .customerId("CUST-008")
                .customerEmail("customer8@example.com")
                .idempotencyKey(idempotencyKey)
                .build();

        PaymentDto authResponse = webTestClient.post()
                .uri("/api/v1/payments/authorize")
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(authRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(PaymentDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(authResponse);
        Long paymentId = authResponse.getId();

        webTestClient.get()
                .uri("/api/v1/payments/order/{orderId}", 8L)
                .header("Authorization", "Bearer test-token")
                .exchange()
                .expectStatus().isOk()
                .expectBody(PaymentDto.class)
                .value(response -> {
                    assertEquals(paymentId, response.getId());
                });
    }
}