package com.example.product.fuzz;

import com.example.product.ProductApplication;
import com.example.product.TestSecurityConfig;
import com.example.product.config.RabbitMQConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.reactive.ReactiveOAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.security.reactive.ReactiveManagementWebSecurityAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for JQF validation fuzz tests for Product service.
 * Provides common setup for fuzzing controller endpoints with malformed JSON
 * and asserting RFC 7807 400 responses.
 */
@Testcontainers
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {ProductApplication.class, TestSecurityConfig.class, RabbitMQConfig.class}
)
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, RabbitMQConfig.class, ValidationFuzzTest.FuzzTestConfig.class})
@EnableAutoConfiguration(exclude = {
    ReactiveSecurityAutoConfiguration.class,
    ReactiveOAuth2ResourceServerAutoConfiguration.class,
    ReactiveManagementWebSecurityAutoConfiguration.class
})
@ComponentScan(
    basePackages = {"com.example.product", "com.example.common.exception"},
    excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.example.product.pact.*")
)
@EnableMongoRepositories(basePackages = "com.example.product")
public abstract class ValidationFuzzTest {

    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

    @Container
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:3.13-management-alpine")
            .withExposedPorts(5672, 15672);

    static {
        MONGO.start();
        RABBITMQ.start();
    }

    @LocalServerPort
    protected int port;

    @Autowired
    protected ObjectMapper objectMapper;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", MONGO::getConnectionString);
        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBITMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBITMQ::getAdminPassword);
        registry.add("spring.rabbitmq.publisher-confirm-type", () -> "correlated");
        registry.add("spring.rabbitmq.publisher-returns", () -> "true");
        registry.add("eureka.client.enabled", () -> "false");
    }

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/";
    }

    /**
     * Test configuration - no outbox publisher needed for product service.
     */
    @Configuration
    static class FuzzTestConfig {
        // Product service doesn't use outbox publisher
    }

    /**
     * Sends a POST request with the given JSON payload and asserts RFC 7807 400 response.
     *
     * @param endpoint the endpoint path (e.g., "/products")
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
     * @param endpoint the endpoint path (e.g., "/products/1")
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