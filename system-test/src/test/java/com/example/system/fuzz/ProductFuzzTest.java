package com.example.system.fuzz;

import com.example.common.dto.ProductDto;
import com.example.common.dto.ProductVariantDto;
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
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * JQF fuzz tests for ProductController and ProductVariantController endpoints.
 * Tests POST /products, PUT /products/{id}, POST /products/{productId}/variants,
 * PUT /products/{productId}/variants/sku/{skuCode} with malformed JSON payloads.
 */
@RunWith(JQF.class)
public class ProductFuzzTest extends ValidationFuzzTest {

    private static final String VALID_PRODUCT_JSON = """
            {
                "name": "Test Product",
                "description": "A test product",
                "categoryId": "1",
                "attributes": {}
            }
            """;

    private static final String VALID_VARIANT_JSON = """
            {
                "skuCode": "TEST-SKU-001",
                "name": "Test Variant",
                "price": 99.99,
                "attributes": {},
                "inventoryQuantity": 100,
                "reservedQuantity": 0,
                "lowStockThreshold": 10
            }
            """;

    @Autowired
    private ObjectMapper objectMapper;

    private String testProductId;

    @BeforeEach
    void setUp() {
        super.setUp();
        // Create a test product for variant tests
        String location = given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(VALID_PRODUCT_JSON)
                .when()
                .post("/products")
                .then()
                .statusCode(201)
                .extract()
                .header("Location");
        testProductId = location.substring(location.lastIndexOf('/') + 1);
    }

    @Fuzz
    @DisplayName("Fuzz POST /products with malformed JSON")
    public void fuzzCreateProduct(String mutatedJson) {
        if (!mutatedJson.equals(VALID_PRODUCT_JSON)) {
            assertValidationError("/products", mutatedJson);
        }
    }

    @Fuzz
    @DisplayName("Fuzz PUT /products/{id} with malformed JSON")
    public void fuzzUpdateProduct(String mutatedJson) {
        if (!mutatedJson.equals(VALID_PRODUCT_JSON)) {
            assertValidationErrorPut("/products/" + testProductId, mutatedJson);
        }
    }

    @Fuzz
    @DisplayName("Fuzz POST /products/{productId}/variants with malformed JSON")
    public void fuzzCreateVariant(String mutatedJson) {
        if (!mutatedJson.equals(VALID_VARIANT_JSON)) {
            assertValidationError("/products/" + testProductId + "/variants", mutatedJson);
        }
    }

    @Fuzz
    @DisplayName("Fuzz PUT /products/{productId}/variants/sku/{skuCode} with malformed JSON")
    public void fuzzUpdateVariant(String mutatedJson) {
        // Create a variant first
        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(VALID_VARIANT_JSON)
                .when()
                .post("/products/" + testProductId + "/variants")
                .then()
                .statusCode(201);

        if (!mutatedJson.equals(VALID_VARIANT_JSON)) {
            assertValidationErrorPut("/products/" + testProductId + "/variants/sku/TEST-SKU-001", mutatedJson);
        }
    }

    @Fuzz
    @DisplayName("Fuzz POST /products with oversized payload")
    public void fuzzCreateProductOversized(String basePayload) {
        String oversized = generateOversizedPayload(basePayload, 10_000_000); // 10MB
        assertValidationError("/products", oversized);
    }

    @Fuzz
    @DisplayName("Fuzz POST /products with numeric overflow on price")
    public void fuzzCreateProductNumericOverflow(String basePayload) {
        // ProductDto doesn't have price directly, but variants do
        String overflow = generateNumericOverflowPayload(basePayload, "price");
        assertValidationError("/products/" + testProductId + "/variants", overflow);
    }

    @Fuzz
    @DisplayName("Fuzz POST /products with deeply nested attributes")
    public void fuzzCreateProductDeepNesting(int depth) {
        String nested = generateDeeplyNestedPayload(Math.min(depth, 1000));
        // Inject into attributes field
        String payload = VALID_PRODUCT_JSON.replace("\"attributes\": {}", "\"attributes\":" + nested);
        assertValidationError("/products", payload);
    }

    @Fuzz
    @DisplayName("Fuzz POST /products with regex bypass on SKU")
    public void fuzzCreateProductRegexBypass(String fieldName, String maliciousValue) {
        String bypass = generateRegexBypassPayload(VALID_VARIANT_JSON, fieldName, maliciousValue);
        assertValidationError("/products/" + testProductId + "/variants", bypass);
    }

    @Fuzz
    @DisplayName("Fuzz variant price with negative values")
    public void fuzzVariantNegativePrice(String basePayload) {
        String negative = basePayload.replace("\"price\": 99.99", "\"price\": -99.99");
        assertValidationError("/products/" + testProductId + "/variants", negative);
    }

    @Fuzz
    @DisplayName("Fuzz variant inventory with overflow")
    public void fuzzVariantInventoryOverflow(String basePayload) {
        String overflow = basePayload.replace("\"inventoryQuantity\": 100", "\"inventoryQuantity\": 2147483648");
        assertValidationError("/products/" + testProductId + "/variants", overflow);
    }

    @Test
    @DisplayName("Valid product creation should succeed")
    void validProductCreation() {
        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(VALID_PRODUCT_JSON)
                .when()
                .post("/products")
                .then()
                .statusCode(201);
    }

    @Test
    @DisplayName("Valid variant creation should succeed")
    void validVariantCreation() {
        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(VALID_VARIANT_JSON)
                .when()
                .post("/products/" + testProductId + "/variants")
                .then()
                .statusCode(201);
    }
}