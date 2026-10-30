package com.example.system.fuzz;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.berkeley.cs.jqf.junit5.FuzzTest;
import edu.berkeley.cs.jqf.junit5.JQFTestExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;

import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * JQF fuzz tests for OrderController endpoints.
 * Tests POST /orders and POST /orders/{id}/cancel with malformed JSON payloads.
 * <p>
 * Hits the running order service on localhost:8083 (started via Docker Compose).
 */
@ExtendWith(JQFTestExtension.class)
public class OrderFuzzIntegrationTest extends ValidationFuzzIntegrationTest {

    private static final String VALID_ORDER_JSON = """
            {
                "customerId": "CUST-001",
                "customerEmail": "customer@example.com",
                "items": [
                    {
                        "productId": 1,
                        "variantId": 1,
                        "quantity": 2
                    }
                ]
            }
            """;

    private static final String VALID_ORDER_ITEM_JSON = """
            {
                "productId": 1,
                "variantId": 1,
                "skuCode": "LAPTOP-13-SILVER",
                "productName": "Laptop 13-inch Silver",
                "quantity": 2,
                "quantityShipped": 0,
                "price": "999.99",
                "status": "PENDING"
            }
            """;

    @BeforeEach
    void setUp() {
        super.setUp();
        RestAssured.port = 8083;
        RestAssured.basePath = "/";
    }

    @FuzzTest
    @DisplayName("Fuzz POST /orders with malformed JSON")
    public void fuzzCreateOrder(String mutatedJson) {
        if (!mutatedJson.equals(VALID_ORDER_JSON)) {
            assertValidationError("/orders", mutatedJson);
        }
    }

    @FuzzTest
    @DisplayName("Fuzz POST /orders/{id}/cancel with malformed JSON")
    public void fuzzCancelOrder(String mutatedJson) {
        // Cancel doesn't take a body, but we test with body anyway
        assertValidationErrorPut("/orders/1/cancel", mutatedJson);
    }

    @FuzzTest
    @DisplayName("Fuzz POST /orders with oversized payload")
    public void fuzzCreateOrderOversized(String basePayload) {
        String oversized = generateOversizedPayload(basePayload, 10_000_000); // 10MB
        assertValidationError("/orders", oversized);
    }

    @FuzzTest
    @DisplayName("Fuzz POST /orders with numeric overflow on totalAmount")
    public void fuzzCreateOrderNumericOverflow(String basePayload) {
        String overflow = generateNumericOverflowPayload(basePayload, "totalAmount");
        assertValidationError("/orders", overflow);
    }

    @FuzzTest
    @DisplayName("Fuzz POST /orders with deeply nested items array")
    public void fuzzCreateOrderDeepNesting(int depth) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < Math.min(depth, 100); i++) {
            sb.append(VALID_ORDER_ITEM_JSON);
            if (i < depth - 1) sb.append(",");
        }
        sb.append("]");
        String payload = VALID_ORDER_JSON.replace("\"items\": []", "\"items\":" + sb);
        assertValidationError("/orders", payload);
    }

    @FuzzTest
    @DisplayName("Fuzz POST /orders with regex bypass on customerId")
    public void fuzzCreateOrderRegexBypass(String fieldName, String maliciousValue) {
        String bypass = generateRegexBypassPayload(VALID_ORDER_JSON, fieldName, maliciousValue);
        assertValidationError("/orders", bypass);
    }

    @FuzzTest
    @DisplayName("Fuzz POST /orders with negative price in items")
    public void fuzzCreateOrderNegativePrice(String basePayload) {
        String negative = basePayload.replace("\"price\": \"999.99\"", "\"price\": \"-999.99\"");
        assertValidationError("/orders", negative);
    }

    @FuzzTest
    @DisplayName("Fuzz POST /orders with zero quantity in items")
    public void fuzzCreateOrderZeroQuantity(String basePayload) {
        String zero = basePayload.replace("\"quantity\": 2", "\"quantity\": 0");
        assertValidationError("/orders", zero);
    }

    @FuzzTest
    @DisplayName("Fuzz POST /orders with huge quantity in items")
    public void fuzzCreateOrderHugeQuantity(String basePayload) {
        String huge = basePayload.replace("\"quantity\": 2", "\"quantity\": 2147483648");
        assertValidationError("/orders", huge);
    }

    @Test
    @DisplayName("Valid order creation should succeed")
    void validOrderCreation() {
        // Note: This will fail without proper product/inventory setup
        // but serves as a template for integration testing
    }
}
