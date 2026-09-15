package com.example.product.pact;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.ProviderInfo;
import au.com.dius.pact.provider.ConsumerInfo;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(PactVerificationInvocationContextProvider.class)
class ProductPactProviderTest {

    @LocalServerPort
    private int port;

    private static ProviderInfo providerInfo;

    @BeforeAll
    static void setupProvider() {
        providerInfo = new ProviderInfo("product-service");
        providerInfo.setProtocol("http");
        providerInfo.setHost("localhost");
        providerInfo.setPath("/");
        
        providerInfo.hasPactWith("order-service", consumer -> {
            consumer.setPactSource("target/pacts");
            return kotlin.Unit.INSTANCE;
        });
    }

    @BeforeEach
    void before(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
        context.setProviderInfo(providerInfo);
    }

    @TestTemplate
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("valid product create request")
    void validProductCreateRequest(PactDslJsonBody body) {
        body.stringType("name", "Laptop Pro 15")
            .stringType("description", "High-performance laptop for professionals")
            .decimalType("price", "1999.99")
            .stringType("categoryId", "cat-123");
    }

    @State("valid product update request")
    void validProductUpdateRequest(PactDslJsonBody body) {
        body.stringType("name", "Laptop Pro 16")
            .stringType("description", "Updated high-performance laptop")
            .decimalType("price", "2199.99")
            .stringType("categoryId", "cat-123");
    }

    @State("product create request - name blank")
    void productCreateRequestNameBlank(PactDslJsonBody body) {
        body.stringValue("name", "")
            .stringType("description", "High-performance laptop")
            .decimalType("price", "1999.99")
            .stringType("categoryId", "cat-123");
    }

    @State("product create request - name null")
    void productCreateRequestNameNull(PactDslJsonBody body) {
        body.stringType("name", null)
            .stringType("description", "High-performance laptop")
            .decimalType("price", "1999.99")
            .stringType("categoryId", "cat-123");
    }

    @State("product create request - name too long")
    void productCreateRequestNameTooLong(PactDslJsonBody body) {
        body.stringValue("name", "A".repeat(256))
            .stringType("description", "High-performance laptop")
            .decimalType("price", "1999.99")
            .stringType("categoryId", "cat-123");
    }

    @State("product create request - description too long")
    void productCreateRequestDescriptionTooLong(PactDslJsonBody body) {
        body.stringType("name", "Laptop Pro 15")
            .stringValue("description", "A".repeat(1001))
            .decimalType("price", "1999.99")
            .stringType("categoryId", "cat-123");
    }

    @State("product create request - price null")
    void productCreateRequestPriceNull(PactDslJsonBody body) {
        body.stringType("name", "Laptop Pro 15")
            .stringType("description", "High-performance laptop")
            .stringType("price", null)
            .stringType("categoryId", "cat-123");
    }

    @State("product create request - price not positive")
    void productCreateRequestPriceNotPositive(PactDslJsonBody body) {
        body.stringType("name", "Laptop Pro 15")
            .stringType("description", "High-performance laptop")
            .decimalType("price", "0.00")
            .stringType("categoryId", "cat-123");
    }

    @State("product create request - price negative")
    void productCreateRequestPriceNegative(PactDslJsonBody body) {
        body.stringType("name", "Laptop Pro 15")
            .stringType("description", "High-performance laptop")
            .decimalType("price", "-10.00")
            .stringType("categoryId", "cat-123");
    }

    @State("product update request - name blank")
    void productUpdateRequestNameBlank(PactDslJsonBody body) {
        body.stringValue("name", "")
            .stringType("description", "High-performance laptop")
            .decimalType("price", "1999.99")
            .stringType("categoryId", "cat-123");
    }

    @State("product update request - name null")
    void productUpdateRequestNameNull(PactDslJsonBody body) {
        body.stringType("name", null)
            .stringType("description", "High-performance laptop")
            .decimalType("price", "1999.99")
            .stringType("categoryId", "cat-123");
    }

    @State("product update request - name too long")
    void productUpdateRequestNameTooLong(PactDslJsonBody body) {
        body.stringValue("name", "A".repeat(256))
            .stringType("description", "High-performance laptop")
            .decimalType("price", "1999.99")
            .stringType("categoryId", "cat-123");
    }

    @State("product update request - description too long")
    void productUpdateRequestDescriptionTooLong(PactDslJsonBody body) {
        body.stringType("name", "Laptop Pro 15")
            .stringValue("description", "A".repeat(1001))
            .decimalType("price", "1999.99")
            .stringType("categoryId", "cat-123");
    }

    @State("product update request - price null")
    void productUpdateRequestPriceNull(PactDslJsonBody body) {
        body.stringType("name", "Laptop Pro 15")
            .stringType("description", "High-performance laptop")
            .stringType("price", null)
            .stringType("categoryId", "cat-123");
    }

    @State("product update request - price not positive")
    void productUpdateRequestPriceNotPositive(PactDslJsonBody body) {
        body.stringType("name", "Laptop Pro 15")
            .stringType("description", "High-performance laptop")
            .decimalType("price", "0.00")
            .stringType("categoryId", "cat-123");
    }
}