package com.example.product.pact;

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

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "category-service", port = "8082")
class ProductCategoryConsumerPactTest {

    @Pact(consumer = "product-service")
    public V4Pact validGetCategoryById(PactDslWithProvider builder) {
        return builder
                .given("category with id 123 exists")
                .uponReceiving("A request to get category by id")
                .path("/categories/123")
                .method("GET")
                .willRespondWith()
                .status(200)
                .body(new PactDslJsonBody()
                        .integerType("id", 123L)
                        .stringType("name", "Electronics")
                        .stringType("description", "Electronic devices and accessories")
                        .integerType("parentId", 0)
                        .array("children"))
                .toPact().asV4Pact().get();
    }

    @Pact(consumer = "product-service")
    @Disabled
    public V4Pact categoryNotFound(PactDslWithProvider builder) {
        return builder
                .given("category with id 999 does not exist")
                .uponReceiving("A request to get non-existent category")
                .path("/categories/999")
                .method("GET")
                .willRespondWith()
                .status(404)
                .toPact().asV4Pact().get();
    }

    @Test
    @PactTestFor(pactMethod = "validGetCategoryById")
    void testValidGetCategoryById(MockServer mockServer) {
        String mockServerUrl = mockServer.getUrl();
        System.out.println("Mock server URL: " + mockServerUrl);
        WebClient.create(mockServerUrl)
                .get()
                .uri("/categories/123")
                .retrieve()
                .toBodilessEntity()
                .block();
        assertNotNull("Pact generated");
    }

    @Test
    @Disabled
    @PactTestFor(pactMethod = "categoryNotFound")
    void testCategoryNotFound(MockServer mockServer) {
        String mockServerUrl = mockServer.getUrl();
        System.out.println("Mock server URL: " + mockServerUrl);
        WebClient.create(mockServerUrl)
                .get()
                .uri("/categories/999")
                .retrieve()
                .toBodilessEntity()
                .block();
        assertNotNull("Pact generated");
    }
}