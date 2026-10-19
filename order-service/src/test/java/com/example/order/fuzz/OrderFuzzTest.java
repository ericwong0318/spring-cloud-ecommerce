package com.example.order.fuzz;

import com.example.common.event.BaseEvent;
import com.example.common.event.OutboxEventPublisher;
import com.example.common.event.ReactiveIdempotentEventProcessor;
import com.example.order.MinimalTestConfig;
import com.example.order.TestDatabaseMigrations;
import com.example.order.TestSecurityConfig;
import com.example.order.outbox.R2dbcOutboxEventRepository;
import com.example.order.service.OrderEventPublisher;
import edu.berkeley.cs.jqf.junit5.FuzzTest;
import edu.berkeley.cs.jqf.junit5.JQFTestExtension;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;

import java.util.function.Function;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * JQF fuzz tests for the OrderController endpoints.
 *
 * <p>The contract under test:
 * <ul>
 *   <li>a well-formed order is accepted ({@code 200});</li>
 *   <li>malformed or invalid input is rejected with {@code 400} and an RFC 9457
 *       ({@code application/problem+json}) body;</li>
 *   <li>the endpoint never fails with a {@code 5xx} for client-supplied input.</li>
 * </ul>
 *
 * <p>Runs the reactive web stack (matching production) against Testcontainers PostgreSQL
 * (with the Flyway schema applied) and RabbitMQ.
 */
@Testcontainers
@ExtendWith(JQFTestExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {MinimalTestConfig.class, TestSecurityConfig.class, OrderFuzzTest.FuzzTestConfig.class},
    properties = {
        "spring.main.web-application-type=reactive",
        // Read oversized bodies into memory so the server can answer with a problem+json
        // 400 instead of aborting the stream mid-upload.
        "spring.codec.max-in-memory-size=16MB",
        "spring.flyway.enabled=false"
    }
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
        TestDatabaseMigrations.run(POSTGRES);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private OutboxEventPublisher outboxEventPublisher;

    @Autowired
    private OrderEventPublisher eventPublisher;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () -> String.format("r2dbc:postgresql://%s:%d/%s",
                POSTGRES.getHost(), POSTGRES.getFirstMappedPort(), POSTGRES.getDatabaseName()));
        registry.add("spring.r2dbc.username", POSTGRES::getUsername);
        registry.add("spring.r2dbc.password", POSTGRES::getPassword);

        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBITMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBITMQ::getAdminPassword);
        registry.add("spring.rabbitmq.publisher-confirm-type", () -> "correlated");
        registry.add("spring.rabbitmq.publisher-returns", () -> "true");

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

        registry.add("eureka.client.enabled", () -> "false");
    }

    /**
     * Wires the minimal reactive context: R2DBC repositories plus no-op mocks for the
     * event-processing collaborators that are irrelevant to input validation.
     */
    @Configuration
    @EnableR2dbcRepositories(basePackages = "com.example.order.repository")
    static class FuzzTestConfig {

        @Bean
        @Primary
        ReactiveIdempotentEventProcessor reactiveIdempotentEventProcessor() {
            return new ReactiveIdempotentEventProcessor(null, null) {
                @Override
                public <T extends BaseEvent> Mono<Void> process(T event, Function<T, Mono<Void>> handler) {
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
        RestAssured.port = port;
        RestAssured.basePath = "/orders";
        Mockito.when(outboxEventPublisher.saveEvent(anyString(), anyString(), anyString(), any()))
                .thenReturn(Mono.empty());
        Mockito.when(eventPublisher.publishOrderCreated(any())).thenReturn(Mono.empty());
        Mockito.when(eventPublisher.publishOrderUpdated(any())).thenReturn(Mono.empty());
        Mockito.when(eventPublisher.publishOrderCancelled(any())).thenReturn(Mono.empty());
    }

    // ------------------------------------------------------------------
    // Deterministic contract tests
    // ------------------------------------------------------------------

    @Test
    @DisplayName("A valid order is accepted")
    void validOrderCreation() {
        given()
                .contentType(ContentType.JSON)
                .body(VALID_ORDER_JSON)
                .when()
                .post()
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("Malformed JSON is rejected with a problem+json 400")
    void malformedJsonIsRejected() {
        assertProblemDetail(postFor("", "{ not json"));
    }

    @Test
    @DisplayName("A missing required field is rejected with a problem+json 400")
    void missingRequiredFieldIsRejected() {
        assertProblemDetail(postFor("", """
                { "customerEmail": "customer@example.com", "items": [] }
                """));
    }

    @Test
    @DisplayName("A missing customer email is rejected with a problem+json 400")
    void missingCustomerEmailIsRejected() {
        assertProblemDetail(postFor("", """
                { "customerId": "CUST-001", "items": [] }
                """));
    }

    @Test
    @DisplayName("An over-long customer email is rejected with a problem+json 400")
    void overLongCustomerEmailIsRejected() {
        String longEmail = "a".repeat(256) + "@example.com";
        assertProblemDetail(postFor("", """
                { "customerId": "CUST-001", "customerEmail": "%s", "items": [] }
                """.formatted(longEmail)));
    }

    @Test
    @DisplayName("An oversized payload is rejected with a problem+json 400")
    void oversizedPayloadIsRejected() {
        assertProblemDetail(postFor("", generateOversizedPayload("{", 1_000_000)));
    }

    // ------------------------------------------------------------------
    // Fuzz tests: the endpoint must never 5xx on arbitrary client input,
    // and every 4xx response must be a problem+json document.
    // ------------------------------------------------------------------

    @FuzzTest
    @DisplayName("Fuzz POST /orders with arbitrary bodies")
    void fuzzCreateOrder(String payload) {
        assertNeverServerError(postFor("", payload));
    }

    @FuzzTest
    @DisplayName("Fuzz POST /orders with oversized bodies")
    void fuzzCreateOrderOversized(String payload) {
        assertNeverServerError(postFor("", generateOversizedPayload(payload, 1_000_000)));
    }

    @FuzzTest
    @DisplayName("Fuzz POST /orders with deeply nested bodies")
    void fuzzCreateOrderDeepNesting(int depth) {
        String nested = generateDeeplyNestedPayload(Math.min(Math.abs(depth), 200));
        String payload = "{\"customerId\":\"CUST-001\",\"customerEmail\":\"customer@example.com\","
                + "\"items\":" + nested + "}";
        assertNeverServerError(postFor("", payload));
    }

    @FuzzTest
    @DisplayName("Fuzz POST /orders with extreme quantities")
    void fuzzCreateOrderHugeQuantity(int quantity) {
        String payload = String.format(
                "{\"customerId\":\"CUST-001\",\"customerEmail\":\"customer@example.com\","
                        + "\"items\":[{\"productId\":1,\"variantId\":1,\"quantity\":%d}]}",
                quantity);
        assertNeverServerError(postFor("", payload));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Response postFor(String endpoint, String body) {
        return given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(endpoint)
                .then()
                .extract()
                .response();
    }

    private void assertNeverServerError(Response response) {
        assertThat(response.statusCode())
                .as("status code for body: %s", abbreviate(response))
                .isLessThan(500);
        if (response.statusCode() >= 400) {
            assertRfc7807Response(response);
        }
    }

    private void assertProblemDetail(Response response) {
        assertThat(response.statusCode()).as("status code").isEqualTo(400);
        assertRfc7807Response(response);
    }

    private void assertRfc7807Response(Response response) {
        assertThat(response.getContentType()).contains("application/problem+json");

        String body = response.getBody().asString();
        assertThat(body).contains("\"type\"");
        assertThat(body).contains("\"title\"");
        assertThat(body).contains("\"status\"");
        assertThat(body).contains("\"detail\"");
        assertThat(body).contains("\"instance\"");
        assertThat(response.jsonPath().getInt("status")).isBetween(400, 499);
    }

    private String abbreviate(Response response) {
        String body = response.getBody() == null ? "" : response.getBody().asString();
        return body.length() > 120 ? body.substring(0, 120) + "..." : body;
    }

    private String generateOversizedPayload(String basePayload, int targetSize) {
        StringBuilder sb = new StringBuilder(basePayload == null ? "" : basePayload);
        while (sb.length() < targetSize) {
            sb.append("padding");
        }
        return sb.toString();
    }

    private String generateDeeplyNestedPayload(int depth) {
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
}
