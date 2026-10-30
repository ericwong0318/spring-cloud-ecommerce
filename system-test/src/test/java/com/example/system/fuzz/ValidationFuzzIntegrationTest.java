package com.example.system.fuzz;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import edu.berkeley.cs.jqf.junit5.JQFTestExtension;

import java.util.Map;

import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for JQF validation fuzz tests.
 * <p>
 * These are system-level integration tests that hit running services via
 * RestAssured. No embedded Spring context is started — all services are
 * expected to be up (via Docker Compose) before these tests run.
 */
@ExtendWith(JQFTestExtension.class)
public abstract class ValidationFuzzIntegrationTest {

    protected final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // RestAssured.port is set per-service in each subclass's @BeforeEach
    }

    /**
     * Sends a POST request with the given JSON payload and asserts RFC 7807 400 response.
     *
     * @param endpoint the endpoint path (e.g., "/categories")
     * @param jsonPayload the JSON payload to send
     */
    protected void assertValidationError(String endpoint, String jsonPayload) {
        Response response = given()
                .contentType(ContentType.JSON)
                .body(jsonPayload)
                .when()
                .post(endpoint)
                .then()
                .statusCode(400)
                .extract()
                .response();

        assertRfc7807Response(response);
    }

    /**
     * Sends a PUT request with the given JSON payload and asserts RFC 7807 400 response.
     *
     * @param endpoint the endpoint path (e.g., "/categories/1")
     * @param jsonPayload the JSON payload to send
     */
    protected void assertValidationErrorPut(String endpoint, String jsonPayload) {
        Response response = given()
                .contentType(ContentType.JSON)
                .body(jsonPayload)
                .when()
                .put(endpoint)
                .then()
                .statusCode(400)
                .extract()
                .response();

        assertRfc7807Response(response);
    }

    /**
     * Asserts that the response follows RFC 7807 Problem Details format.
     * Accepts both application/problem+json and application/json content types,
     * as long as the status code is 400 and the body contains status=400.
     *
     * @param response the RestAssured response
     */
    protected void assertRfc7807Response(Response response) {
        String contentType = response.getContentType();
        assertThat(contentType).matches(".*application/(problem\\+)?json.*");

        String body = response.getBody().asString();
        assertThat(body).contains("\"status\"");
        Map<String, Object> problemDetails = response.jsonPath().getMap("$");
        assertThat(problemDetails.get("status")).isEqualTo(400);
    }

    /**
     * Generates a malformed JSON payload by applying mutations to the valid payload.
     * This method can be overridden by subclasses to provide service-specific mutations.
     *
     * @param validPayload the valid JSON payload
     * @return a mutated JSON payload
     */
    protected String mutatePayload(String validPayload) {
        // Default implementation - subclasses should override
        return validPayload;
    }

    /**
     * Generates oversized payload to test size limits.
     *
     * @param basePayload the base payload
     * @param targetSize target size in bytes
     * @return oversized payload
     */
    protected String generateOversizedPayload(String basePayload, int targetSize) {
        StringBuilder sb = new StringBuilder(basePayload);
        while (sb.length() < targetSize) {
            sb.append("padding");
        }
        return sb.toString();
    }

    /**
     * Generates numeric overflow payloads for integer/long fields.
     *
     * @param basePayload the base payload
     * @param fieldName the field name to overflow
     * @return payload with overflow value
     */
    protected String generateNumericOverflowPayload(String basePayload, String fieldName) {
        // Replace the field value with Long.MAX_VALUE + 1 (as string)
        return basePayload.replaceAll("\"" + fieldName + "\"\\s*:\\s*\\d+",
                "\"" + fieldName + "\": " + (Long.MAX_VALUE + 1L));
    }

    /**
     * Generates deeply nested object payload to test nesting limits.
     *
     * @param depth nesting depth
     * @return deeply nested JSON
     */
    protected String generateDeeplyNestedPayload(int depth) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < depth; i++) {
            sb.append("\"nested").append(i).append("\":{");
        }
        sb.append("\"value\":\"test\"");
        for (int i = 0; i < depth; i++) {
            sb.append("}");
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Generates payload with regex false negatives/positives.
     * Tests validation regex edge cases.
     *
     * @param basePayload the base payload
     * @param fieldName the field name with regex validation
     * @param maliciousValue value that might bypass regex
     * @return payload with malicious value
     */
    protected String generateRegexBypassPayload(String basePayload, String fieldName, String maliciousValue) {
        return basePayload.replaceAll("\"" + fieldName + "\"\\s*:\\s*\"[^\"]*\"",
                "\"" + fieldName + "\":\"" + maliciousValue + "\"");
    }
}
