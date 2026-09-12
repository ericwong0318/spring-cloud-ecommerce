package com.example.system.fuzz;

import com.example.common.dto.CategoryDto;
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
 * JQF fuzz tests for CategoryController endpoints.
 * Tests POST /categories and PUT /categories/{id} with malformed JSON payloads.
 */
@RunWith(JQF.class)
public class CategoryFuzzTest extends ValidationFuzzTest {

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

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        super.setUp();
        // Create a parent category for tests that need parentId
        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(VALID_CATEGORY_JSON)
                .when()
                .post("/categories")
                .then()
                .statusCode(201);
    }

    @Fuzz
    @DisplayName("Fuzz POST /categories with malformed JSON")
    public void fuzzCreateCategory(String mutatedJson) {
        // Only test if the mutated JSON is different from valid (to avoid false positives)
        if (!mutatedJson.equals(VALID_CATEGORY_JSON) && !mutatedJson.equals(VALID_CATEGORY_WITH_PARENT_JSON)) {
            assertValidationError("/categories", mutatedJson);
        }
    }

    @Fuzz
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

    @Fuzz
    @DisplayName("Fuzz POST /categories with oversized payload")
    public void fuzzCreateCategoryOversized(String basePayload) {
        String oversized = generateOversizedPayload(basePayload, 10_000_000); // 10MB
        assertValidationError("/categories", oversized);
    }

    @Fuzz
    @DisplayName("Fuzz POST /categories with numeric overflow")
    public void fuzzCreateCategoryNumericOverflow(String basePayload) {
        String overflow = generateNumericOverflowPayload(basePayload, "parentId");
        assertValidationError("/categories", overflow);
    }

    @Fuzz
    @DisplayName("Fuzz POST /categories with deeply nested objects")
    public void fuzzCreateCategoryDeepNesting(int depth) {
        String nested = generateDeeplyNestedPayload(Math.min(depth, 1000));
        assertValidationError("/categories", nested);
    }

    @Fuzz
    @DisplayName("Fuzz POST /categories with regex bypass attempts")
    public void fuzzCreateCategoryRegexBypass(String fieldName, String maliciousValue) {
        String bypass = generateRegexBypassPayload(VALID_CATEGORY_JSON, fieldName, maliciousValue);
        assertValidationError("/categories", bypass);
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