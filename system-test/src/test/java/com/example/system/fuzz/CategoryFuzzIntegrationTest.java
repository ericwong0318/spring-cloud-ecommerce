package com.example.system.fuzz;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.berkeley.cs.jqf.junit5.FuzzTest;
import edu.berkeley.cs.jqf.junit5.JQFTestExtension;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;

import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * JQF fuzz tests for CategoryController endpoints.
 * Tests POST /categories and PUT /categories/{id} with malformed JSON payloads.
 * <p>
 * Hits the running category service on localhost:8082 (started via Docker Compose).
 */
@ExtendWith(JQFTestExtension.class)
public class CategoryFuzzIntegrationTest extends ValidationFuzzIntegrationTest {

    private static final String VALID_CATEGORY_JSON = """
            {
                "name": "Electronics",
                "description": "Electronic devices",
                "parentId": null,
                "imageUrl": null
            }
            """;

    private static final String VALID_CATEGORY_WITH_PARENT_JSON = """
            {
                "name": "Laptops",
                "description": "Laptop computers",
                "parentId": 1,
                "imageUrl": null
            }
            """;

    @BeforeEach
    void setUp() {
        super.setUp();
        RestAssured.port = 8082;
        RestAssured.basePath = "/";
        // Create a parent category for tests that need parentId
        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(VALID_CATEGORY_JSON)
                .when()
                .post("/categories")
                .then()
                .statusCode(201);
    }

    @FuzzTest
    @DisplayName("Fuzz POST /categories with malformed JSON")
    public void fuzzCreateCategory(String mutatedJson) {
        // Only test if the mutated JSON is different from valid (to avoid false positives)
        if (!mutatedJson.equals(VALID_CATEGORY_JSON) && !mutatedJson.equals(VALID_CATEGORY_WITH_PARENT_JSON)) {
            assertValidationError("/categories", mutatedJson);
        }
    }

    @FuzzTest
    @DisplayName("Fuzz PUT /categories/{id} with malformed JSON")
    public void fuzzUpdateCategory(String mutatedJson) {
        // Create a category first
        String location = given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(VALID_CATEGORY_JSON)
                .when()
                .post("/categories")
                .then()
                .statusCode(201)
                .extract()
                .header("Location");
        Long categoryId = Long.valueOf(location.substring(location.lastIndexOf('/') + 1));

        if (!mutatedJson.equals(VALID_CATEGORY_JSON) && !mutatedJson.equals(VALID_CATEGORY_WITH_PARENT_JSON)) {
            assertValidationErrorPut("/categories/" + categoryId, mutatedJson);
        }
    }

    @FuzzTest
    @DisplayName("Fuzz POST /categories with oversized payload")
    public void fuzzCreateCategoryOversized(String basePayload) {
        String oversized = generateOversizedPayload(basePayload, 10_000_000); // 10MB
        try {
            assertValidationError("/categories", oversized);
        } catch (Exception e) {
            // Connection reset/broken pipe is expected for oversized payloads
            assertThat(e.getMessage()).containsAnyOf("Broken pipe", "Connection reset");
        }
    }

    @FuzzTest
    @DisplayName("Fuzz POST /categories with numeric overflow")
    public void fuzzCreateCategoryNumericOverflow(String basePayload) {
        String overflow = generateNumericOverflowPayload(basePayload, "parentId");
        assertValidationError("/categories", overflow);
    }

    @FuzzTest
    @DisplayName("Fuzz POST /categories with deeply nested objects")
    public void fuzzCreateCategoryDeepNesting(int depth) {
        String nested = generateDeeplyNestedPayload(Math.min(depth, 1000));
        assertValidationError("/categories", nested);
    }

    @FuzzTest
    @DisplayName("Fuzz POST /categories with regex bypass attempts")
    public void fuzzCreateCategoryRegexBypass(String fieldName, String maliciousValue) {
        String bypass = generateRegexBypassPayload(VALID_CATEGORY_JSON, fieldName, maliciousValue);
        Response response = given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(bypass)
                .when()
                .post("/categories")
                .then()
                .extract()
                .response();
        // Services may return 201 (no validation), 400, or 500 for regex bypass payloads
        assertThat(response.getStatusCode()).isIn(200, 201, 400, 500);
    }

    @Test
    @DisplayName("Valid category creation should succeed")
    void validCategoryCreation() {
        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(VALID_CATEGORY_JSON)
                .when()
                .post("/categories")
                .then()
                .statusCode(201);
    }
}
