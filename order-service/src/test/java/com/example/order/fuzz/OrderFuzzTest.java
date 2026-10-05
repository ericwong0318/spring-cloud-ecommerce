package com.example.order.fuzz;

import com.example.common.dto.CreateOrderRequest;
import com.example.common.dto.CreateOrderItemRequest;
import com.example.order.MinimalTestConfig;
import com.example.order.TestSecurityConfig;
import com.example.order.outbox.R2dbcOutboxEventRepository;
import com.example.common.event.ReactiveIdempotentEventProcessor;
import com.example.common.event.BaseEvent;
import com.example.common.event.OutboxEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.berkeley.cs.jqf.junit5.FuzzTest;
import edu.berkeley.cs.jqf.junit5.JQFTestExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.mockito.Mockito;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import io.r2dbc.spi.ConnectionFactory;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * JQF fuzz tests for OrderController endpoints.
 * Tests POST /orders and POST /orders/{id}/cancel with malformed JSON payloads.
 * 
 * Uses Testcontainers for PostgreSQL and RabbitMQ infrastructure.
 */
@Testcontainers
@ExtendWith(JQFTestExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {MinimalTestConfig.class, TestSecurityConfig.class, OrderFuzzTest.FuzzTestConfig.class}
)
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, OrderFuzzTest.FuzzTestConfig.class})
@EnableAutoConfiguration(exclude = {
    SecurityAutoConfiguration.class,
    OAuth2ResourceServerAutoConfiguration.class,
    ManagementWebSecurityAutoConfiguration.class
})
public class OrderFuzzTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("order_fuzz_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:3.13-management-alpine")
            .withExposedPorts(5672, 15672);

    static {
        POSTGRES.start();
        RABBITMQ.start();
    }

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL (R2DBC)
        registry.add("spring.r2dbc.url", () -> String.format("r2dbc:postgresql://%s:%d/%s",
                POSTGRES.getHost(), POSTGRES.getFirstMappedPort(), POSTGRES.getDatabaseName()));
        registry.add("spring.r2dbc.username", POSTGRES::getUsername);
        registry.add("spring.r2dbc.password", POSTGRES::getPassword);

        // RabbitMQ
        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBITMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBITMQ::getAdminPassword);
        registry.add("spring.rabbitmq.publisher-confirm-type", () -> "correlated");
        registry.add("spring.rabbitmq.publisher-returns", () -> "true");
        registry.add("rabbitmq.exchange.order", () -> "order.exchange");
        registry.add("rabbitmq.exchange.payment", () -> "payment.exchange");
        registry.add("rabbitmq.exchange.inventory", () -> "inventory.exchange");
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

        // Other
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    /**
     * Test configuration to mock the ReactiveIdempotentEventProcessor and related components.
     */
    @Configuration
    static class FuzzTestConfig {
        @Bean
        @Primary
        ReactiveIdempotentEventProcessor reactiveIdempotentEventProcessor() {
            return new ReactiveIdempotentEventProcessor(null, null) {
                @Override
                public <T extends BaseEvent> Mono<Void> process(T event, java.util.function.Function<T, Mono<Void>> handler) {
                    return handler.apply(event);
                }
            };
        }

        @Bean
        @Primary
        R2dbcOutboxEventRepository r2dbcOutboxEventRepository() {
            return Mockito.mock(R2dbcOutboxEventRepository.class);
        }

        @Bean
        @Primary
        TransactionalOperator transactionalOperator(ConnectionFactory connectionFactory) {
            R2dbcTransactionManager transactionManager = new R2dbcTransactionManager(connectionFactory);
            return TransactionalOperator.create(transactionManager);
        }
    }

    private static final String VALID_ORDER_JSON = """
            {
                "customerId": "CUST-001",
                "customerEmail": "customer@example.com",
                "items": [
                    {
                        "productId": 1,
                        "variantId": 1,
                        "quantity": 2
                    }
                ]
            }
            """;

    @BeforeEach
    void setUp() {
        io.restassured.RestAssured.port = port;
        io.restassured.RestAssured.basePath = "/orders";
    }

    @FuzzTest
    @DisplayName("Fuzz POST /orders with malformed JSON")
    public void fuzzCreateOrder(String mutatedJson) {
        if (!mutatedJson.equals(VALID_ORDER_JSON)) {
            assertValidationError("", mutatedJson);
        }
    }

    @FuzzTest
    @DisplayName("Fuzz POST /orders/{id}/cancel with malformed JSON")
    public void fuzzCancelOrder(String mutatedJson) {
        // Create an order first
        String location = given()
                .contentType("application/json")
                .body(VALID_ORDER_JSON)
                .when()
                .post()
                .then()
                .statusCode(200)
                .extract()
                .header("Location");
        Long orderId = Long.valueOf(location.substring(location.lastIndexOf('/') + 1));

        assertValidationErrorPut("/" + orderId + "/cancel", mutatedJson);
    }

    @FuzzTest
    @DisplayName("Fuzz POST /orders with oversized payload")
    public void fuzzCreateOrderOversized(String basePayload) {
        String oversized = generateOversizedPayload(basePayload, 10_000_000); // 10MB
        assertValidationError("", oversized);
    }

    @FuzzTest
    @DisplayName("Fuzz POST /orders with numeric overflow on totalAmount")
    public void fuzzCreateOrderNumericOverflow(String basePayload) {
        String overflow = generateNumericOverflowPayload(basePayload, "totalAmount");
        assertValidationError("", overflow);
    }

    @FuzzTest
    @DisplayName("Fuzz POST /orders with deeply nested items array")
    public void fuzzCreateOrderDeepNesting(int depth) {
        String nested = generateDeeplyNestedPayload(Math.min(depth, 100));
        String payload = VALID_ORDER_JSON.replace("\"items\": [{", "\"items\":" + nested + "[");
        assertValidationError("", payload);
    }

    @FuzzTest
    @DisplayName("Fuzz POST /orders with regex bypass on customerId")
    public void fuzzCreateOrderRegexBypass(String fieldName, String maliciousValue) {
        String bypass = generateRegexBypassPayload(VALID_ORDER_JSON, fieldName, maliciousValue);
        assertValidationError("", bypass);
    }

    @FuzzTest
    @DisplayName("Fuzz POST /orders with negative price in items")
    public void fuzzCreateOrderNegativePrice(String basePayload) {
        String negative = basePayload.replace("\"price\": \"999.99\"", "\"price\": \"-999.99\"");
        assertValidationError("", negative);
    }

    @FuzzTest
    @DisplayName("Fuzz POST /orders with zero quantity in items")
    public void fuzzCreateOrderZeroQuantity(String basePayload) {
        String zero = basePayload.replace("\"quantity\": 2", "\"quantity\": 0");
        assertValidationError("", zero);
    }

    @FuzzTest
    @DisplayName("Fuzz POST /orders with huge quantity in items")
    public void fuzzCreateOrderHugeQuantity(String basePayload) {
        String huge = basePayload.replace("\"quantity\": 2", "\"quantity\": 2147483648");
        assertValidationError("", huge);
    }

    @Test
    @DisplayName("Valid order creation should succeed")
    void validOrderCreation() {
        given()
                .contentType("application/json")
                .body(VALID_ORDER_JSON)
                .when()
                .post()
                .then()
                .statusCode(200);
    }

    // ============================================================
    // Validation helper methods
    // ============================================================

    protected void assertValidationError(String endpoint, String jsonPayload) {
        io.restassured.response.Response response = given()
                .contentType(io.restassured.http.ContentType.JSON)
                .body(jsonPayload)
                .when()
                .post(endpoint)
                .then()
                .statusCode(400)
                .extract()
                .response();

        assertRfc7807Response(response);
    }

    protected void assertValidationErrorPut(String endpoint, String jsonPayload) {
        io.restassured.response.Response response = given()
                .contentType(io.restassured.http.ContentType.JSON)
                .body(jsonPayload)
                .when()
                .put(endpoint)
                .then()
                .statusCode(400)
                .extract()
                .response();

        assertRfc7807Response(response);
    }

    protected void assertRfc7807Response(io.restassured.response.Response response) {
        String contentType = response.getContentType();
        assertThat(contentType).contains("application/problem+json");

        String body = response.getBody().asString();
        assertThat(body).contains("type");
        assertThat(body).contains("title");
        assertThat(body).contains("status");
        assertThat(body).contains("detail");
        assertThat(body).contains("instance");

        java.util.Map<String, Object> problemDetails = response.jsonPath().getMap("$");
        assertThat(problemDetails.get("status")).isEqualTo(400);
    }

    protected String generateOversizedPayload(String basePayload, int targetSize) {
        StringBuilder sb = new StringBuilder(basePayload);
        while (sb.length() < targetSize) {
            sb.append("padding");
        }
        return sb.toString();
    }

    protected String generateNumericOverflowPayload(String basePayload, String fieldName) {
        return basePayload.replaceAll("\"" + fieldName + "\"\\s*:\\s*\\d+",
                "\"" + fieldName + "\": " + (Long.MAX_VALUE + 1L));
    }

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

    protected String generateRegexBypassPayload(String basePayload, String fieldName, String maliciousValue) {
        return basePayload.replaceAll("\"" + fieldName + "\"\\s*:\\s*\"[^\"]*\"",
                "\"" + fieldName + "\":\"" + maliciousValue + "\"");
    }
}