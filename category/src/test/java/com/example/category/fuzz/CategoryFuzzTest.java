package com.example.category.fuzz;

import com.example.category.CategoryApplication;
import com.example.category.TestSecurityConfig;
import com.example.common.dto.CategoryDto;
import com.example.common.event.OutboxEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.berkeley.cs.jqf.junit5.FuzzTest;
import edu.berkeley.cs.jqf.junit5.JQFTestExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * JQF fuzz tests for CategoryController endpoints.
 * Tests POST /categories and PUT /categories/{id} with malformed JSON payloads.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {CategoryApplication.class, TestSecurityConfig.class}
)
@ExtendWith(JQFTestExtension.class)
public class CategoryFuzzTest extends ValidationFuzzTest {

    @MockBean
    private OutboxEventPublisher outboxEventPublisher;

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
        // Inject an oversized value into a bounded field so the request is rejected
        // by Jakarta Validation (@Size). Padding appended after the JSON object is
        // ignored by Jackson, and multi-MB bodies are dropped by the connector as a
        // broken pipe rather than a 400 response.
        String oversized = VALID_CATEGORY_JSON.replace("\"Electronics\"",
                "\"" + "x".repeat(10_000) + "\"");
        assertValidationError("/categories", oversized);
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
    @DisplayName("Fuzz POST /categories with an over-length name (size-limit bypass)")
    public void fuzzCreateCategoryNameSizeBypass(String fieldName, String maliciousValue) {
        // CategoryDto declares no @Pattern constraints, so there is no regex to
        // bypass. The meaningful boundary is @Size(max = 255) on the name field;
        // inject a value beyond it and assert it is rejected deterministically.
        String oversizedName = "x".repeat(300);
        String bypass = generateRegexBypassPayload(VALID_CATEGORY_JSON, "name", oversizedName);
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