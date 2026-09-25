package com.example.notification.pact;

import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "order-service", port = "8083")
class OrderServiceConsumerPactTest {

    private final WebTestClient webTestClient = WebTestClient.bindToServer()
            .baseUrl("http://localhost:8083")
            .build();

    @Pact(consumer = "notification-service", provider = "order-service")
    public V4Pact createOrderPact(au.com.dius.pact.consumer.dsl.PactDslWithProvider builder) {
        return builder
            .given("valid order creation request")
            .uponReceiving("a request to create an order")
                .path("/orders")
                .method("POST")
                .headers(Map.of("Content-Type", "application/json"))
                .body("{\"customerId\":\"cust-123\",\"items\":[{\"productId\":1,\"quantity\":2,\"price\":29.99}]}")
            .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body("{\"id\":1,\"customerId\":\"cust-123\",\"items\":[{\"productId\":1,\"quantity\":2,\"price\":29.99}],\"status\":\"CREATED\"}")
            .toPact(V4Pact.class);
    }

    @Pact(consumer = "notification-service", provider = "order-service")
    public V4Pact getOrderPact(au.com.dius.pact.consumer.dsl.PactDslWithProvider builder) {
        return builder
            .given("order with id 1 exists")
            .uponReceiving("a request to get an order")
                .path("/orders/1")
                .method("GET")
            .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body("{\"id\":1,\"customerId\":\"cust-123\",\"items\":[{\"productId\":1,\"quantity\":2,\"price\":29.99}],\"status\":\"CREATED\"}")
            .toPact(V4Pact.class);
    }

    @Pact(consumer = "notification-service", provider = "order-service")
    public V4Pact cancelOrderPact(au.com.dius.pact.consumer.dsl.PactDslWithProvider builder) {
        return builder
            .given("order with id 1 exists and can be cancelled")
            .uponReceiving("a request to cancel an order")
                .path("/orders/1/cancel")
                .method("POST")
            .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body("{\"id\":1,\"customerId\":\"cust-123\",\"items\":[{\"productId\":1,\"quantity\":2,\"price\":29.99}],\"status\":\"CANCELLED\"}")
            .toPact(V4Pact.class);
    }


    @Test
    @PactTestFor(pactMethod = "createOrderPact")
    void testCreateOrder() {
        webTestClient.post()
            .uri("/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"customerId\":\"cust-123\",\"items\":[{\"productId\":1,\"quantity\":2,\"price\":29.99}]}")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id").isEqualTo(1)
            .jsonPath("$.customerId").isEqualTo("cust-123");
    }

    @Test
    @PactTestFor(pactMethod = "getOrderPact")
    void testGetOrder() {
        webTestClient.get()
            .uri("/orders/1")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id").isEqualTo(1)
            .jsonPath("$.customerId").isEqualTo("cust-123");
    }

    @Test
    @PactTestFor(pactMethod = "cancelOrderPact")
    void testCancelOrder() {
        webTestClient.post()
            .uri("/orders/1/cancel")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id").isEqualTo(1)
            .jsonPath("$.status").isEqualTo("CANCELLED");
    }
}
