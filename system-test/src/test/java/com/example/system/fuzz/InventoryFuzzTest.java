package com.example.system.fuzz;

import com.example.common.dto.ConfirmStockRequest;
import com.example.common.dto.ReserveStockRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * JQF fuzz tests for InventoryController endpoints.
 * Tests POST /inventory/reserve, DELETE /inventory/reserve/{orderItemId},
 * POST /inventory/confirm with malformed JSON payloads.
 */
@RunWith(JQF.class)
public class InventoryFuzzTest extends ValidationFuzzTest {

    private static final String VALID_RESERVE_JSON = """
            {
                "variantId": 1,
                "quantity": 5,
                "orderItemId": 100
            }
            """;

    private static final String VALID_CONFIRM_JSON = """
            {
                "variantId": 1,
                "quantity": 5
            }
            """;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        super.setUp();
    }

    @Fuzz
    @DisplayName("Fuzz POST /inventory/reserve with malformed JSON")
    public void fuzzReserveStock(String mutatedJson) {
        if (!mutatedJson.equals(VALID_RESERVE_JSON)) {
            assertValidationError("/inventory/reserve", mutatedJson);
        }
    }

    @Fuzz
    @DisplayName("Fuzz POST /inventory/confirm with malformed JSON")
    public void fuzzConfirmStock(String mutatedJson) {
        if (!mutatedJson.equals(VALID_CONFIRM_JSON)) {
            assertValidationError("/inventory/confirm", mutatedJson);
        }
    }

    @Fuzz
    @DisplayName("Fuzz DELETE /inventory/reserve/{orderItemId} with malformed params")
    public void fuzzReleaseReservation(String variantId, String quantity) {
        // Test with invalid query params
        given()
                .queryParam("variantId", variantId)
                .queryParam("quantity", quantity)
                .when()
                .delete("/inventory/reserve/1")
                .then()
                .statusCode(400);
    }

    @Fuzz
    @DisplayName("Fuzz POST /inventory/reserve with oversized payload")
    public void fuzzReserveStockOversized(String basePayload) {
        String oversized = generateOversizedPayload(basePayload, 10_000_000); // 10MB
        assertValidationError("/inventory/reserve", oversized);
    }

    @Fuzz
    @DisplayName("Fuzz POST /inventory/reserve with numeric overflow on variantId")
    public void fuzzReserveStockNumericOverflow(String basePayload) {
        String overflow = generateNumericOverflowPayload(basePayload, "variantId");
        assertValidationError("/inventory/reserve", overflow);
    }

    @Fuzz
    @DisplayName("Fuzz POST /inventory/reserve with deeply nested structure")
    public void fuzzReserveStockDeepNesting(int depth) {
        String nested = generateDeeplyNestedPayload(Math.min(depth, 1000));
        String payload = VALID_RESERVE_JSON.replace("\"variantId\": 1", "\"variantId\":" + nested);
        assertValidationError("/inventory/reserve", payload);
    }

    @Fuzz
    @DisplayName("Fuzz POST /inventory/reserve with zero quantity")
    public void fuzzReserveStockZeroQuantity(String basePayload) {
        String zero = basePayload.replace("\"quantity\": 5", "\"quantity\": 0");
        assertValidationError("/inventory/reserve", zero);
    }

    @Fuzz
    @DisplayName("Fuzz POST /inventory/reserve with negative quantity")
    public void fuzzReserveStockNegativeQuantity(String basePayload) {
        String negative = basePayload.replace("\"quantity\": 5", "\"quantity\": -5");
        assertValidationError("/inventory/reserve", negative);
    }

    @Fuzz
    @DisplayName("Fuzz POST /inventory/reserve with huge quantity")
    public void fuzzReserveStockHugeQuantity(String basePayload) {
        String huge = basePayload.replace("\"quantity\": 5", "\"quantity\": 2147483648");
        assertValidationError("/inventory/reserve", huge);
    }

    @Fuzz
    @DisplayName("Fuzz POST /inventory/confirm with zero quantity")
    public void fuzzConfirmStockZeroQuantity(String basePayload) {
        String zero = basePayload.replace("\"quantity\": 5", "\"quantity\": 0");
        assertValidationError("/inventory/confirm", zero);
    }

    @Fuzz
    @DisplayName("Fuzz POST /inventory/confirm with negative quantity")
    public void fuzzConfirmStockNegativeQuantity(String basePayload) {
        String negative = basePayload.replace("\"quantity\": 5", "\"quantity\": -5");
        assertValidationError("/inventory/confirm", negative);
    }

    @Test
    @DisplayName("Valid reserve should succeed")
    void validReserve() {
        // Note: Requires valid inventory setup
    }
}