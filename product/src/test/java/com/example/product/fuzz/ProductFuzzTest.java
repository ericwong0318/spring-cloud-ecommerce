package com.example.product.fuzz;

import com.example.common.dto.ProductDto;
import com.example.common.dto.ProductVariantDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.berkeley.cs.jqf.junit5.FuzzTest;
import edu.berkeley.cs.jqf.junit5.JQFTestExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.testcontainers.junit.jupiter.Testcontainers;

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
@ExtendWith(JQFTestExtension.class)
@Testcontainers
public class ProductFuzzTest extends ValidationFuzzTest {

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

    @Autowired
    private ObjectMapper objectMapper;

    private String testProductId;

    @BeforeEach
    void setUp() {
        super.setUp();
        // Create a test product for variant tests
        String response = given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(VALID_PRODUCT_JSON)
                .when()
                .post("/products")
                .then()
                .log().all()
                .statusCode(201)
                .extract()
                .asString();
        
        // Extract product ID from response
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
        if (!mutatedJson.equals(VALID_VARIANT_JSON)) {
            assertValidationErrorPut("/products/" + testProductId + "/variants/sku/TEST-SKU-001", mutatedJson);
        }
    }

    @FuzzTest
    @DisplayName("Fuzz oversized payload")
    public void fuzzOversizedPayload(String basePayload) {
        String oversized = generateOversizedPayload(VALID_PRODUCT_JSON, 10000);
        assertValidationError("/products", oversized);
    }

    @FuzzTest
    @DisplayName("Fuzz numeric overflow on price")
    public void fuzzNumericOverflow(String basePayload) {
        String overflow = generateNumericOverflowPayload(VALID_PRODUCT_JSON, "price");
        assertValidationError("/products", overflow);
    }

    @FuzzTest
    @DisplayName("Fuzz deep nesting in attributes")
    public void fuzzDeepNesting(String basePayload) {
        String deep = generateDeeplyNestedPayload(50);
        assertValidationError("/products", deep);
    }

    @FuzzTest
    @DisplayName("Fuzz regex bypass attempt on categoryId")
    public void fuzzRegexBypass(String basePayload) {
        String bypass = generateRegexBypassPayload(VALID_PRODUCT_JSON, "categoryId", "cat-<script>alert(1)</script>");
        assertValidationError("/products", bypass);
    }

    @Test
    @DisplayName("Valid product creation should return 201")
    void validProductCreation() {
        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(VALID_PRODUCT_JSON)
                .when()
                .post("/products")
                .then()
                .statusCode(201)
                .body("name", org.hamcrest.Matchers.equalTo("Test Product"));
    }

    @Test
    @DisplayName("Valid variant creation should return 201")
    void validVariantCreation() {
        String variantJson = """
                {
                    "skuCode": "TEST-SKU-002",
                    "name": "Test Variant 2",
                    "price": 49.99,
                    "attributes": {},
                    "inventoryQuantity": 50,
                    "reservedQuantity": 0,
                    "lowStockThreshold": 5
                }
                """;
        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(variantJson)
                .when()
                .post("/products/" + testProductId + "/variants")
                .then()
                .statusCode(201)
                .body("skuCode", org.hamcrest.Matchers.equalTo("TEST-SKU-002"));
    }
}
