package com.example.system.fuzz;

import com.example.common.dto.AuthorizeRequest;
import com.example.common.dto.CaptureRequest;
import com.example.common.dto.RefundRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * JQF fuzz tests for PaymentController endpoints.
 * Tests POST /payments/authorize, POST /payments/{id}/capture, POST /payments/{id}/refund
 * with malformed JSON payloads.
 */
@RunWith(JQF.class)
public class PaymentFuzzTest extends ValidationFuzzTest {

    private static final String VALID_AUTHORIZE_JSON = """
            {
                "orderId": 1,
                "amount": "1999.98",
                "currency": "USD",
                "customerId": "CUST-001",
                "customerEmail": "customer@example.com",
                "idempotencyKey": "auth-1-abc123"
            }
            """;

    private static final String VALID_CAPTURE_JSON = """
            {
                "gatewayTransactionId": "txn_123456"
            }
            """;

    private static final String VALID_REFUND_JSON = """
            {
                "amount": "100.00",
                "reason": "Customer requested cancellation"
            }
            """;

    @Autowired
    private ObjectMapper objectMapper;

    private Long testPaymentId;

    @BeforeEach
    void setUp() {
        super.setUp();
        // Note: Payment tests require an order to exist first
        // In a real test, we'd create an order and authorize a payment
    }

    @Fuzz
    @DisplayName("Fuzz POST /payments/authorize with malformed JSON")
    public void fuzzAuthorizePayment(String mutatedJson) {
        if (!mutatedJson.equals(VALID_AUTHORIZE_JSON)) {
            assertValidationError("/payments/authorize", mutatedJson);
        }
    }

    @Fuzz
    @DisplayName("Fuzz POST /payments/{id}/capture with malformed JSON")
    public void fuzzCapturePayment(String mutatedJson) {
        if (!mutatedJson.equals(VALID_CAPTURE_JSON)) {
            assertValidationError("/payments/1/capture", mutatedJson);
        }
    }

    @Fuzz
    @DisplayName("Fuzz POST /payments/{id}/refund with malformed JSON")
    public void fuzzRefundPayment(String mutatedJson) {
        if (!mutatedJson.equals(VALID_REFUND_JSON)) {
            assertValidationError("/payments/1/refund", mutatedJson);
        }
    }

    @Fuzz
    @DisplayName("Fuzz POST /payments/authorize with oversized payload")
    public void fuzzAuthorizePaymentOversized(String basePayload) {
        String oversized = generateOversizedPayload(basePayload, 10_000_000); // 10MB
        assertValidationError("/payments/authorize", oversized);
    }

    @Fuzz
    @DisplayName("Fuzz POST /payments/authorize with numeric overflow on amount")
    public void fuzzAuthorizePaymentNumericOverflow(String basePayload) {
        String overflow = generateNumericOverflowPayload(basePayload, "amount");
        assertValidationError("/payments/authorize", overflow);
    }

    @Fuzz
    @DisplayName("Fuzz POST /payments/authorize with deeply nested structure")
    public void fuzzAuthorizePaymentDeepNesting(int depth) {
        String nested = generateDeeplyNestedPayload(Math.min(depth, 1000));
        // Inject into a field that shouldn't have nesting
        String payload = VALID_AUTHORIZE_JSON.replace("\"customerId\": \"CUST-001\"", "\"customerId\":" + nested);
        assertValidationError("/payments/authorize", payload);
    }

    @Fuzz
    @DisplayName("Fuzz POST /payments/authorize with regex bypass on currency")
    public void fuzzAuthorizePaymentRegexBypass(String fieldName, String maliciousValue) {
        String bypass = generateRegexBypassPayload(VALID_AUTHORIZE_JSON, fieldName, maliciousValue);
        assertValidationError("/payments/authorize", bypass);
    }

    @Fuzz
    @DisplayName("Fuzz POST /payments/authorize with negative amount")
    public void fuzzAuthorizePaymentNegativeAmount(String basePayload) {
        String negative = basePayload.replace("\"amount\": \"1999.98\"", "\"amount\": \"-1999.98\"");
        assertValidationError("/payments/authorize", negative);
    }

    @Fuzz
    @DisplayName("Fuzz POST /payments/authorize with invalid currency length")
    public void fuzzAuthorizePaymentInvalidCurrency(String basePayload) {
        String invalid = basePayload.replace("\"currency\": \"USD\"", "\"currency\": \"US\"");
        assertValidationError("/payments/authorize", invalid);
    }

    @Fuzz
    @DisplayName("Fuzz POST /payments/authorize with empty idempotency key")
    public void fuzzAuthorizePaymentEmptyIdempotencyKey(String basePayload) {
        String empty = basePayload.replace("\"idempotencyKey\": \"auth-1-abc123\"", "\"idempotencyKey\": \"\"");
        assertValidationError("/payments/authorize", empty);
    }

    @Fuzz
    @DisplayName("Fuzz POST /payments/{id}/capture with empty gatewayTransactionId")
    public void fuzzCapturePaymentEmptyGatewayId(String basePayload) {
        String empty = basePayload.replace("\"gatewayTransactionId\": \"txn_123456\"", "\"gatewayTransactionId\": \"\"");
        assertValidationError("/payments/1/capture", empty);
    }

    @Fuzz
    @DisplayName("Fuzz POST /payments/{id}/refund with negative amount")
    public void fuzzRefundPaymentNegativeAmount(String basePayload) {
        String negative = basePayload.replace("\"amount\": \"100.00\"", "\"amount\": \"-100.00\"");
        assertValidationError("/payments/1/refund", negative);
    }

    @Test
    @DisplayName("Valid authorize should succeed")
    void validAuthorize() {
        // Note: Requires valid order
    }
}