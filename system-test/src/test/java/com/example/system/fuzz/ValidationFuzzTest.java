package com.example.system.fuzz;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;

import com.example.common.event.JpaOutboxEventPublisher;
import com.example.common.event.ReactiveOutboxEventPublisher;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for JQF validation fuzz tests.
 * Provides common setup for fuzzing controller endpoints with malformed JSON
 * and asserting RFC 7807 400 responses.
 */
@Testcontainers
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {
        ValidationFuzzTest.TestConfig.class
    }
)
public abstract class ValidationFuzzTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("ecommerce_fuzz_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static final MongoDBContainer MONGODB = new MongoDBContainer("mongo:7.0")
            .withReuse(true);

    @Container
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:3.13-management-alpine")
            .withExposedPorts(5672, 15672);

    @LocalServerPort
    protected int port;

    @Autowired
    protected ObjectMapper objectMapper;

    @Value("${local.server.port}")
    protected int localServerPort;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");

        // R2DBC
        registry.add("spring.r2dbc.url", () -> String.format("r2dbc:postgresql://%s:%d/%s",
                POSTGRES.getHost(), POSTGRES.getFirstMappedPort(), POSTGRES.getDatabaseName()));
        registry.add("spring.r2dbc.username", POSTGRES::getUsername);
        registry.add("spring.r2dbc.password", POSTGRES::getPassword);

        // MongoDB (for product service)
        registry.add("spring.data.mongodb.uri", MONGODB::getReplicaSetUrl);
        registry.add("spring.data.mongodb.database", () -> "product_fuzz_test_db");
        registry.add("spring.data.mongodb.auto-index-creation", () -> "true");

        // RabbitMQ
        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBITMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBITMQ::getAdminPassword);

        // RabbitMQ properties for order service
        registry.add("rabbitmq.exchange.order", () -> "order.exchange");
        registry.add("rabbitmq.exchange.payment", () -> "payment.exchange");
        registry.add("rabbitmq.exchange.inventory", () -> "inventory.exchange");
        registry.add("rabbitmq.exchange.ecommerce", () -> "ecommerce.exchange");
        registry.add("rabbitmq.queue.order-events", () -> "order.events.queue");
        registry.add("rabbitmq.queue.payment-events", () -> "payment.events.queue");
        registry.add("rabbitmq.queue.inventory-events", () -> "inventory.events.queue");
        registry.add("rabbitmq.queue.reservation-expired", () -> "reservation.expired.queue");
        registry.add("rabbitmq.routing-key.order-created", () -> "order.created");
        registry.add("rabbitmq.routing-key.order-updated", () -> "order.updated");
        registry.add("rabbitmq.routing-key.order-cancelled", () -> "order.cancelled");
        registry.add("rabbitmq.routing-key.payment-authorized", () -> "payment.authorized");
        registry.add("rabbitmq.routing-key.payment-captured", () -> "payment.captured");
        registry.add("rabbitmq.routing-key.payment-refunded", () -> "payment.refunded");
        registry.add("rabbitmq.routing-key.payment-failed", () -> "payment.failed");
        registry.add("rabbitmq.routing-key.inventory-reserved", () -> "inventory.reserved");
        registry.add("rabbitmq.routing-key.reservation-expired", () -> "reservation.expired");

        // Disable service discovery and config server
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.config.import", () -> "optional:configserver:");

        // Disable security for tests
        registry.add("spring.autoconfigure.exclude", () -> "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration," +
                "org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration," +
                "org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration," +
                "org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration," +
                "com.example.common.event.ReactiveOutboxEventPublisherAutoConfiguration");
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

    @Configuration
    @SpringBootApplication
    @ComponentScan(
        basePackages = {
            "com.example.category",
            "com.example.product",
            "com.example.order",
            "com.example.inventory",
            "com.example.payment",
            "com.example.notification"
        }
    )
    static class TestConfig {
    }
}