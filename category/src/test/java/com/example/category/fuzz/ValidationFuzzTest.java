package com.example.category.fuzz;

import com.example.category.CategoryApplication;
import com.example.category.TestSecurityConfig;
import com.example.common.event.OutboxEventPublisher;
import com.example.common.event.OutboxEventRepository;
import com.example.common.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.context.annotation.Import;
import reactor.core.publisher.Mono;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for JQF validation fuzz tests for Category service.
 * Provides common setup for fuzzing controller endpoints with malformed JSON
 * and asserting RFC 7807 400 responses.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {CategoryApplication.class, TestSecurityConfig.class}
)
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
@EnableAutoConfiguration(exclude = {
    SecurityAutoConfiguration.class,
    OAuth2ResourceServerAutoConfiguration.class,
    ManagementWebSecurityAutoConfiguration.class,
    org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.r2dbc.R2dbcDataAutoConfiguration.class
})
// Component scanning and JPA repository scanning are already provided by
// CategoryApplication (the @SpringBootTest configuration class). Declaring them
// again here registers the same repository beans twice and fails the context
// with a BeanDefinitionOverrideException.
public abstract class ValidationFuzzTest {

    @LocalServerPort
    protected int port;

    @Autowired
    protected ObjectMapper objectMapper;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.config.import", () -> "optional:configserver:");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/";
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
     *
     * @param response the RestAssured response
     */
    protected void assertRfc7807Response(Response response) {
        String contentType = response.getContentType();
        assertThat(contentType).contains("application/problem+json");

        String body = response.getBody().asString();
        assertThat(body).contains("type");
        assertThat(body).contains("title");
        assertThat(body).contains("status");
        assertThat(body).contains("detail");
        assertThat(body).contains("instance");

        Map<String, Object> problemDetails = response.jsonPath().getMap("$");
        assertThat(problemDetails.get("status")).isEqualTo(400);
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