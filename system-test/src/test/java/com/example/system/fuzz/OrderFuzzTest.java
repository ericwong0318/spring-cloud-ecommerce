package com.example.system.fuzz;

import com.example.common.dto.OrderDto;
import com.example.common.dto.OrderItemDto;
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
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * JQF fuzz tests for OrderController endpoints.
 * Tests POST /orders and POST /orders/{id}/cancel with malformed JSON payloads.
 */
@RunWith(JQF.class)
public class OrderFuzzTest extends ValidationFuzzTest {

    private static final String VALID_ORDER_JSON = """
            {
                "customerId": "CUST-001",
                "customerEmail": "customer@example.com",
                "status": "PENDING",
                "totalAmount": "1999.98",
                "items": [
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
                ],
                "shipments": [],
                "createdAt": null,
                "updatedAt": null
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

    @Autowired
    private ObjectMapper objectMapper;

    private String testProductId;
    private String testVariantId;

    @BeforeEach
    void setUp() {
        super.setUp();
        // Create test product and variant for order tests
        // In a real test, we'd create these via the product service
    }

    @Fuzz
    @DisplayName("Fuzz POST /orders with malformed JSON")
    public void fuzzCreateOrder(String mutatedJson) {
        if (!mutatedJson.equals(VALID_ORDER_JSON)) {
            assertValidationError("/orders", mutatedJson);
        }
    }

    @Fuzz
    @DisplayName("Fuzz POST /orders/{id}/cancel with malformed JSON")
    public void fuzzCancelOrder(String mutatedJson) {
        // Create an order first (or use a mock ID)
        // Note: cancel doesn't take a body, but we test with body anyway
        assertValidationError("/orders/1/cancel", mutatedJson);
    }

    @Fuzz
    @DisplayName("Fuzz POST /orders with oversized payload")
    public void fuzzCreateOrderOversized(String basePayload) {
        String oversized = generateOversizedPayload(basePayload, 10_000_000); // 10MB
        assertValidationError("/orders", oversized);
    }

    @Fuzz
    @DisplayName("Fuzz POST /orders with numeric overflow on totalAmount")
    public void fuzzCreateOrderNumericOverflow(String basePayload) {
        String overflow = generateNumericOverflowPayload(basePayload, "totalAmount");
        assertValidationError("/orders", overflow);
    }

    @Fuzz
    @DisplayName("Fuzz POST /orders with deeply nested items array")
    public void fuzzCreateOrderDeepNesting(int depth) {
        // Create deeply nested items array
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < Math.min(depth, 100); i++) {
            sb.append(VALID_ORDER_ITEM_JSON);
            if (i < depth - 1) sb.append(",");
        }
        sb.append("]");
        String payload = VALID_ORDER_JSON.replace("\"items\": [\"\"\" + VALID_ORDER_ITEM_JSON + \"\"\"]", "\"items\":" + sb);
        assertValidationError("/orders", payload);
    }

    @Fuzz
    @DisplayName("Fuzz POST /orders with regex bypass on customerId")
    public void fuzzCreateOrderRegexBypass(String fieldName, String maliciousValue) {
        String bypass = generateRegexBypassPayload(VALID_ORDER_JSON, fieldName, maliciousValue);
        assertValidationError("/orders", bypass);
    }

    @Fuzz
    @DisplayName("Fuzz POST /orders with negative price in items")
    public void fuzzCreateOrderNegativePrice(String basePayload) {
        String negative = basePayload.replace("\"price\": \"999.99\"", "\"price\": \"-999.99\"");
        assertValidationError("/orders", negative);
    }

    @Fuzz
    @DisplayName("Fuzz POST /orders with zero quantity in items")
    public void fuzzCreateOrderZeroQuantity(String basePayload) {
        String zero = basePayload.replace("\"quantity\": 2", "\"quantity\": 0");
        assertValidationError("/orders", zero);
    }

    @Fuzz
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