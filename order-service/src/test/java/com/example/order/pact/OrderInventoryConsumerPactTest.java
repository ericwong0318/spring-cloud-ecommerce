package com.example.order.pact;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "inventory-service", port = "8084")
class OrderInventoryConsumerPactTest {

    @Pact(consumer = "order-service")
    public V4Pact validReserveStockRequest(PactDslWithProvider builder) {
        return builder
                .given("valid reserve stock request")
                .uponReceiving("A request to reserve stock")
                .path("/inventory/reserve")
                .method("POST")
                .body(new PactDslJsonBody()
                        .integerType("variantId", 1L)
                        .integerType("quantity", 5)
                        .integerType("orderItemId", 100L))
                .willRespondWith()
                .status(200)
                .body(new PactDslJsonBody()
                        .integerType("id", 1L)
                        .integerType("variantId", 1L)
                        .integerType("productId", 1L)
                        .stringType("productName", "Laptop Pro 15")
                        .integerType("quantity", 100)
                        .integerType("reservedQuantity", 5)
                        .integerType("availableQuantity", 95)
                        .integerType("reorderLevel", 10)
                        .decimalType("costPrice", new BigDecimal("500.00"))
                        .booleanType("lowStock", false)
                        .stringType("createdAt", "2024-01-15T10:30:00")
                        .stringType("updatedAt", "2024-01-15T10:30:00"))
                .toPact().asV4Pact().get();
    }

    @Pact(consumer = "order-service")
    @Disabled
    public V4Pact validConfirmStockRequest(PactDslWithProvider builder) {
        return builder
                .given("valid confirm stock request")
                .uponReceiving("A request to confirm stock")
                .path("/inventory/confirm")
                .method("POST")
                .body(new PactDslJsonBody()
                        .integerType("variantId", 1L)
                        .integerType("quantity", 5))
                .willRespondWith()
                .status(200)
                .body(new PactDslJsonBody()
                        .integerType("id", 1L)
                        .integerType("variantId", 1L)
                        .integerType("productId", 1L)
                        .stringType("productName", "Laptop Pro 15")
                        .integerType("quantity", 95)
                        .integerType("reservedQuantity", 0)
                        .integerType("availableQuantity", 95)
                        .integerType("reorderLevel", 10)
                        .decimalType("costPrice", new BigDecimal("500.00"))
                        .booleanType("lowStock", false)
                        .stringType("createdAt", "2024-01-15T10:30:00")
                        .stringType("updatedAt", "2024-01-15T10:30:00"))
                .toPact().asV4Pact().get();
    }

    @Pact(consumer = "order-service")
    @Disabled
    public V4Pact reserveStockRequestVariantIdNull(PactDslWithProvider builder) {
        return builder
                .given("reserve stock request - variantId null")
                .uponReceiving("A request to reserve stock with null variantId")
                .path("/inventory/reserve")
                .method("POST")
                .body(new PactDslJsonBody()
                        .stringType("variantId", null)
                        .integerType("quantity", 5)
                        .integerType("orderItemId", 100L))
                .willRespondWith()
                .status(400)
                .toPact().asV4Pact().get();
    }

    @Pact(consumer = "order-service")
    @Disabled
    public V4Pact reserveStockRequestQuantityNull(PactDslWithProvider builder) {
        return builder
                .given("reserve stock request - quantity null")
                .uponReceiving("A request to reserve stock with null quantity")
                .path("/inventory/reserve")
                .method("POST")
                .body(new PactDslJsonBody()
                        .integerType("variantId", 1L)
                        .stringType("quantity", null)
                        .integerType("orderItemId", 100L))
                .willRespondWith()
                .status(400)
                .toPact().asV4Pact().get();
    }

    @Pact(consumer = "order-service")
    @Disabled
    public V4Pact reserveStockRequestQuantityLessThanOne(PactDslWithProvider builder) {
        return builder
                .given("reserve stock request - quantity less than 1")
                .uponReceiving("A request to reserve stock with quantity less than 1")
                .path("/inventory/reserve")
                .method("POST")
                .body(new PactDslJsonBody()
                        .integerType("variantId", 1L)
                        .integerType("quantity", 0)
                        .integerType("orderItemId", 100L))
                .willRespondWith()
                .status(400)
                .toPact().asV4Pact().get();
    }

    @Pact(consumer = "order-service")
    @Disabled
    public V4Pact reserveStockRequestOrderItemIdNull(PactDslWithProvider builder) {
        return builder
                .given("reserve stock request - orderItemId null")
                .uponReceiving("A request to reserve stock with null orderItemId")
                .path("/inventory/reserve")
                .method("POST")
                .body(new PactDslJsonBody()
                        .integerType("variantId", 1L)
                        .integerType("quantity", 5)
                        .stringType("orderItemId", null))
                .willRespondWith()
                .status(200)
                .body(new PactDslJsonBody()
                        .integerType("id", 1L)
                        .integerType("variantId", 1L)
                        .integerType("productId", 1L)
                        .stringType("productName", "Laptop Pro 15")
                        .integerType("quantity", 100)
                        .integerType("reservedQuantity", 5)
                        .integerType("availableQuantity", 95)
                        .integerType("reorderLevel", 10)
                        .decimalType("costPrice", new BigDecimal("500.00"))
                        .booleanType("lowStock", false)
                        .stringType("createdAt", "2024-01-15T10:30:00")
                        .stringType("updatedAt", "2024-01-15T10:30:00"))
                .toPact().asV4Pact().get();
    }

    @Pact(consumer = "order-service")
    @Disabled
    public V4Pact confirmStockRequestVariantIdNull(PactDslWithProvider builder) {
        return builder
                .given("confirm stock request - variantId null")
                .uponReceiving("A request to confirm stock with null variantId")
                .path("/inventory/confirm")
                .method("POST")
                .body(new PactDslJsonBody()
                        .stringType("variantId", null)
                        .integerType("quantity", 5))
                .willRespondWith()
                .status(400)
                .toPact().asV4Pact().get();
    }

    @Pact(consumer = "order-service")
    @Disabled
    public V4Pact confirmStockRequestQuantityNull(PactDslWithProvider builder) {
        return builder
                .given("confirm stock request - quantity null")
                .uponReceiving("A request to confirm stock with null quantity")
                .path("/inventory/confirm")
                .method("POST")
                .body(new PactDslJsonBody()
                        .integerType("variantId", 1L)
                        .stringType("quantity", null))
                .willRespondWith()
                .status(400)
                .toPact().asV4Pact().get();
    }

    @Pact(consumer = "order-service")
    @Disabled
    public V4Pact confirmStockRequestQuantityLessThanOne(PactDslWithProvider builder) {
        return builder
                .given("confirm stock request - quantity less than 1")
                .uponReceiving("A request to confirm stock with quantity less than 1")
                .path("/inventory/confirm")
                .method("POST")
                .body(new PactDslJsonBody()
                        .integerType("variantId", 1L)
                        .integerType("quantity", 0))
                .willRespondWith()
                .status(400)
                .toPact().asV4Pact().get();
    }

    @Test
    @PactTestFor(pactMethod = "validReserveStockRequest")
    void testValidReserveStockRequest(MockServer mockServer) {
        record ReserveStockRequest(Long variantId, Integer quantity, Long orderItemId) {}
        String mockServerUrl = mockServer.getUrl();
        System.out.println("Mock server URL: " + mockServerUrl);
        WebClient.create(mockServerUrl)
                .post()
                .uri("/inventory/reserve")
                .header("Content-Type", "application/json; charset=UTF-8")
                .bodyValue(new ReserveStockRequest(1L, 5, 100L))
                .retrieve()
                .toBodilessEntity()
                .block();
        assertNotNull("Pact generated");
    }

    @Test
    @PactTestFor(pactMethod = "validConfirmStockRequest")
    void testValidConfirmStockRequest(MockServer mockServer) {
        record ConfirmStockRequest(Long variantId, Integer quantity) {}
        String mockServerUrl = mockServer.getUrl();
        System.out.println("Mock server URL: " + mockServerUrl);
        WebClient.create(mockServerUrl)
                .post()
                .uri("/inventory/confirm")
                .header("Content-Type", "application/json; charset=UTF-8")
                .bodyValue(new ConfirmStockRequest(1L, 5))
                .retrieve()
                .toBodilessEntity()
                .block();
        assertNotNull("Pact generated");
    }
}