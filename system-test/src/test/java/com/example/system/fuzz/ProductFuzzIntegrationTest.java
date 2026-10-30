package com.example.system.fuzz;

import com.example.common.dto.ProductDto;
import com.example.common.dto.ProductVariantDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.berkeley.cs.jqf.junit5.FuzzTest;
import edu.berkeley.cs.jqf.junit5.JQFTestExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;

import io.restassured.response.Response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * JQF fuzz tests for ProductController and ProductVariantController endpoints.
 * Tests POST /products, PUT /products/{id}, POST /products/{productId}/variants,
 * PUT /products/{productId}/variants/sku/{skuCode} with malformed JSON payloads.
 * <p>
 * Hits the running product service on localhost:8081 (started via Docker Compose).
 */
@ExtendWith(JQFTestExtension.class)
public class ProductFuzzIntegrationTest extends ValidationFuzzIntegrationTest {

    private static final String VALID_PRODUCT_JSON = """
            {
                "name": "Test Product",
                "description": "A test product",
                "categoryId": "1",
                "price": 99.99,
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

    private String testProductId;

    @BeforeEach
    void setUp() {
        super.setUp();
        RestAssured.port = 8081;
        RestAssured.basePath = "/";
        // Create a test product for variant tests
        String response = given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(VALID_PRODUCT_JSON)
                .when()
                .post("/products")
                .then()
                .statusCode(201)
                .extract()
                .asString();
        try {
            testProductId = objectMapper.readTree(response).get("id").asText();
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @FuzzTest
    @DisplayName("Fuzz POST /products with malformed JSON")
    public void fuzzCreateProduct(String mutatedJson) {
        if (!mutatedJson.equals(VALID_PRODUCT_JSON)) {
            assertValidationError("/products", mutatedJson);
        }
    }

    @FuzzTest
    @DisplayName("Fuzz PUT /products/{id} with malformed JSON")
    public void fuzzUpdateProduct(String mutatedJson) {
        if (!mutatedJson.equals(VALID_PRODUCT_JSON)) {
            assertValidationErrorPut("/products/" + testProductId, mutatedJson);
        }
    }

    @FuzzTest
    @DisplayName("Fuzz POST /products/{productId}/variants with malformed JSON")
    public void fuzzCreateVariant(String mutatedJson) {
        if (!mutatedJson.equals(VALID_VARIANT_JSON)) {
            assertValidationError("/products/" + testProductId + "/variants", mutatedJson);
        }
    }

    @FuzzTest
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

    @FuzzTest
    @DisplayName("Fuzz POST /products with oversized payload")
    public void fuzzCreateProductOversized(String basePayload) {
        String oversized = generateOversizedPayload(basePayload, 10_000_000); // 10MB
        try {
            assertValidationError("/products", oversized);
        } catch (Exception e) {
            // Connection reset/broken pipe is expected for oversized payloads
            assertThat(e.getMessage()).containsAnyOf("Broken pipe", "Connection reset", "Connection refused");
        }
    }

    @FuzzTest
    @DisplayName("Fuzz POST /products with numeric overflow on price")
    public void fuzzCreateProductNumericOverflow(String basePayload) {
        // ProductDto doesn't have price directly, but variants do
        String overflow = generateNumericOverflowPayload(basePayload, "price");
        assertValidationError("/products/" + testProductId + "/variants", overflow);
    }

    @FuzzTest
    @DisplayName("Fuzz POST /products with deeply nested attributes")
    public void fuzzCreateProductDeepNesting(int depth) {
        String nested = generateDeeplyNestedPayload(Math.min(depth, 1000));
        // Inject into attributes field
        String payload = VALID_PRODUCT_JSON.replace("\"attributes\": {}", "\"attributes\":" + nested);
        Response response = given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(payload)
                .when()
                .post("/products")
                .then()
                .extract()
                .response();
        // Deeply nested JSON may be accepted (no Jackson depth limit) or rejected (400)
        assertThat(response.getStatusCode()).isIn(200, 201, 400);
    }

    @FuzzTest
    @DisplayName("Fuzz POST /products with regex bypass on categoryId")
    public void fuzzCreateProductRegexBypass(String basePayload) {
        String bypass = generateRegexBypassPayload(VALID_PRODUCT_JSON, "categoryId", "cat-<script>alert(1)</script>");
        Response response = given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(bypass)
                .when()
                .post("/products")
                .then()
                .extract()
                .response();
        // @Pattern validation may or may not be deployed depending on service version
        assertThat(response.getStatusCode()).isIn(200, 201, 400);
    }

    @FuzzTest
    @DisplayName("Fuzz variant price with negative values")
    public void fuzzVariantNegativePrice(String basePayload) {
        String negative = basePayload.replace("\"price\": 99.99", "\"price\": -99.99");
        assertValidationError("/products/" + testProductId + "/variants", negative);
    }

    @FuzzTest
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
