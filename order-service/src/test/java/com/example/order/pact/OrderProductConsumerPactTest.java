package com.example.order.pact;

import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "product-service", port = "8081")
class OrderProductConsumerPactTest {

    @Pact(consumer = "order-service")
    public V4Pact validProductCreateRequest(PactDslWithProvider builder) {
        return builder
                .given("valid product create request")
                .uponReceiving("A request to create a product")
                .path("/api/products")
                .method("POST")
                .body(new PactDslJsonBody()
                        .stringType("name", "Laptop Pro 15")
                        .stringType("description", "High-performance laptop for professionals")
                        .decimalType("price", new BigDecimal("1999.99"))
                        .stringType("categoryId", "cat-123"))
                .willRespondWith()
                .status(201)
                .body(new PactDslJsonBody()
                        .stringType("id", "1")
                        .stringType("name", "Laptop Pro 15")
                        .stringType("description", "High-performance laptop for professionals")
                        .decimalType("price", new BigDecimal("1999.99"))
                        .stringType("categoryId", "cat-123")
                        .stringType("categoryName", "Electronics"))
                .toPact().asV4Pact().get();
    }

    @Pact(consumer = "order-service")
    @Disabled
    public V4Pact validProductUpdateRequest(PactDslWithProvider builder) {
        return builder
                .given("valid product update request")
                .uponReceiving("A request to update a product")
                .path("/api/products/1")
                .method("PUT")
                .body(new PactDslJsonBody()
                        .stringType("name", "Laptop Pro 16")
                        .stringType("description", "Updated high-performance laptop")
                        .decimalType("price", new BigDecimal("2199.99"))
                        .stringType("categoryId", "cat-123"))
                .willRespondWith()
                .status(200)
                .body(new PactDslJsonBody()
                        .stringType("id", "1")
                        .stringType("name", "Laptop Pro 16")
                        .stringType("description", "Updated high-performance laptop")
                        .decimalType("price", new BigDecimal("2199.99"))
                        .stringType("categoryId", "cat-123")
                        .stringType("categoryName", "Electronics"))
                .toPact().asV4Pact().get();
    }

    @Pact(consumer = "order-service")
    @Disabled
    public V4Pact productCreateRequestNameBlank(PactDslWithProvider builder) {
        return builder
                .given("product create request - name blank")
                .uponReceiving("A request to create a product with blank name")
                .path("/api/products")
                .method("POST")
                .body(new PactDslJsonBody()
                        .stringValue("name", "")
                        .stringType("description", "High-performance laptop")
                        .decimalType("price", new BigDecimal("1999.99"))
                        .stringType("categoryId", "cat-123"))
                .willRespondWith()
                .status(400)
                .toPact().asV4Pact().get();
    }

    @Pact(consumer = "order-service")
    @Disabled
    public V4Pact productCreateRequestNameNull(PactDslWithProvider builder) {
        return builder
                .given("product create request - name null")
                .uponReceiving("A request to create a product with null name")
                .path("/api/products")
                .method("POST")
                .body(new PactDslJsonBody()
                        .stringType("name", null)
                        .stringType("description", "High-performance laptop")
                        .decimalType("price", new BigDecimal("1999.99"))
                        .stringType("categoryId", "cat-123"))
                .willRespondWith()
                .status(400)
                .toPact().asV4Pact().get();
    }

    @Pact(consumer = "order-service")
    @Disabled
    public V4Pact productCreateRequestNameTooLong(PactDslWithProvider builder) {
        return builder
                .given("product create request - name too long")
                .uponReceiving("A request to create a product with name too long")
                .path("/api/products")
                .method("POST")
                .body(new PactDslJsonBody()
                        .stringValue("name", "A".repeat(256))
                        .stringType("description", "High-performance laptop")
                        .decimalType("price", new BigDecimal("1999.99"))
                        .stringType("categoryId", "cat-123"))
                .willRespondWith()
                .status(400)
                .toPact().asV4Pact().get();
    }

    @Pact(consumer = "order-service")
    @Disabled
    public V4Pact productCreateRequestDescriptionTooLong(PactDslWithProvider builder) {
        return builder
                .given("product create request - description too long")
                .uponReceiving("A request to create a product with description too long")
                .path("/api/products")
                .method("POST")
                .body(new PactDslJsonBody()
                        .stringType("name", "Laptop Pro 15")
                        .stringValue("description", "A".repeat(1001))
                        .decimalType("price", new BigDecimal("1999.99"))
                        .stringType("categoryId", "cat-123"))
                .willRespondWith()
                .status(400)
                .toPact().asV4Pact().get();
    }

    @Pact(consumer = "order-service")
    @Disabled
    public V4Pact productCreateRequestPriceNull(PactDslWithProvider builder) {
        return builder
                .given("product create request - price null")
                .uponReceiving("A request to create a product with null price")
                .path("/api/products")
                .method("POST")
                .body(new PactDslJsonBody()
                        .stringType("name", "Laptop Pro 15")
                        .stringType("description", "High-performance laptop")
                        .stringType("price", null)
                        .stringType("categoryId", "cat-123"))
                .willRespondWith()
                .status(400)
                .toPact().asV4Pact().get();
    }

    @Pact(consumer = "order-service")
    @Disabled
    public V4Pact productCreateRequestPriceNotPositive(PactDslWithProvider builder) {
        return builder
                .given("product create request - price not positive")
                .uponReceiving("A request to create a product with price not positive")
                .path("/api/products")
                .method("POST")
                .body(new PactDslJsonBody()
                        .stringType("name", "Laptop Pro 15")
                        .stringType("description", "High-performance laptop")
                        .decimalType("price", new BigDecimal("0.00"))
                        .stringType("categoryId", "cat-123"))
                .willRespondWith()
                .status(400)
                .toPact().asV4Pact().get();
    }

    @Pact(consumer = "order-service")
    @Disabled
    public V4Pact productCreateRequestPriceNegative(PactDslWithProvider builder) {
        return builder
                .given("product create request - price negative")
                .uponReceiving("A request to create a product with negative price")
                .path("/api/products")
                .method("POST")
                .body(new PactDslJsonBody()
                        .stringType("name", "Laptop Pro 15")
                        .stringType("description", "High-performance laptop")
                        .decimalType("price", new BigDecimal("-10.00"))
                        .stringType("categoryId", "cat-123"))
                .willRespondWith()
                .status(400)
                .toPact().asV4Pact().get();
    }

    @Pact(consumer = "order-service")
    @Disabled
    public V4Pact productUpdateRequestNameBlank(PactDslWithProvider builder) {
        return builder
                .given("product update request - name blank")
                .uponReceiving("A request to update a product with blank name")
                .path("/api/products/1")
                .method("PUT")
                .body(new PactDslJsonBody()
                        .stringValue("name", "")
                        .stringType("description", "High-performance laptop")
                        .decimalType("price", new BigDecimal("1999.99"))
                        .stringType("categoryId", "cat-123"))
                .willRespondWith()
                .status(400)
                .toPact().asV4Pact().get();
    }

    @Pact(consumer = "order-service")
    @Disabled
    public V4Pact productUpdateRequestNameNull(PactDslWithProvider builder) {
        return builder
                .given("product update request - name null")
                .uponReceiving("A request to update a product with null name")
                .path("/api/products/1")
                .method("PUT")
                .body(new PactDslJsonBody()
                        .stringType("name", null)
                        .stringType("description", "High-performance laptop")
                        .decimalType("price", new BigDecimal("1999.99"))
                        .stringType("categoryId", "cat-123"))
                .willRespondWith()
                .status(400)
                .toPact().asV4Pact().get();
    }

    @Pact(consumer = "order-service")
    @Disabled
    public V4Pact productUpdateRequestNameTooLong(PactDslWithProvider builder) {
        return builder
                .given("product update request - name too long")
                .uponReceiving("A request to update a product with name too long")
                .path("/api/products/1")
                .method("PUT")
                .body(new PactDslJsonBody()
                        .stringValue("name", "A".repeat(256))
                        .stringType("description", "High-performance laptop")
                        .decimalType("price", new BigDecimal("1999.99"))
                        .stringType("categoryId", "cat-123"))
                .willRespondWith()
                .status(400)
                .toPact().asV4Pact().get();
    }

    @Pact(consumer = "order-service")
    @Disabled
    public V4Pact productUpdateRequestDescriptionTooLong(PactDslWithProvider builder) {
        return builder
                .given("product update request - description too long")
                .uponReceiving("A request to update a product with description too long")
                .path("/api/products/1")
                .method("PUT")
                .body(new PactDslJsonBody()
                        .stringType("name", "Laptop Pro 15")
                        .stringValue("description", "A".repeat(1001))
                        .decimalType("price", new BigDecimal("1999.99"))
                        .stringType("categoryId", "cat-123"))
                .willRespondWith()
                .status(400)
                .toPact().asV4Pact().get();
    }

    @Pact(consumer = "order-service")
    @Disabled
    public V4Pact productUpdateRequestPriceNull(PactDslWithProvider builder) {
        return builder
                .given("product update request - price null")
                .uponReceiving("A request to update a product with null price")
                .path("/api/products/1")
                .method("PUT")
                .body(new PactDslJsonBody()
                        .stringType("name", "Laptop Pro 15")
                        .stringType("description", "High-performance laptop")
                        .stringType("price", null)
                        .stringType("categoryId", "cat-123"))
                .willRespondWith()
                .status(400)
                .toPact().asV4Pact().get();
    }

    @Pact(consumer = "order-service")
    @Disabled
    public V4Pact productUpdateRequestPriceNotPositive(PactDslWithProvider builder) {
        return builder
                .given("product update request - price not positive")
                .uponReceiving("A request to update a product with price not positive")
                .path("/api/products/1")
                .method("PUT")
                .body(new PactDslJsonBody()
                        .stringType("name", "Laptop Pro 15")
                        .stringType("description", "High-performance laptop")
                        .decimalType("price", new BigDecimal("0.00"))
                        .stringType("categoryId", "cat-123"))
                .willRespondWith()
                .status(400)
                .toPact().asV4Pact().get();
    }

    @Test
    @PactTestFor(pactMethod = "validProductCreateRequest")
    void testValidProductCreateRequest(MockServer mockServer) {
        record ProductCreateRequest(String name, String description, BigDecimal price, String categoryId) {}
        String mockServerUrl = mockServer.getUrl();
        System.out.println("Mock server URL: " + mockServerUrl);
        WebClient.create(mockServerUrl)
                .post()
                .uri("/api/products")
                .header("Content-Type", "application/json; charset=UTF-8")
                .bodyValue(new ProductCreateRequest("Laptop Pro 15", "High-performance laptop for professionals", new BigDecimal("1999.99"), "cat-123"))
                .retrieve()
                .toBodilessEntity()
                .block();
        assertNotNull("Pact generated");
    }
}