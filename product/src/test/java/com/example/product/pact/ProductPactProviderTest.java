package com.example.product.pact;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.junitsupport.State;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Map;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(PactVerificationInvocationContextProvider.class)
@Provider("product-service")
@PactFolder("../order-service/target/pacts")
class ProductPactProviderTest {

    @Container
    static final MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongo::getConnectionString);
    }

    @BeforeEach
    void before(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
    }

    @TestTemplate
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("valid product create request")
    void validProductCreateRequest(Map<String, Object> params) {
        // Provider state setup for valid product create request
    }

    @State("valid product update request")
    void validProductUpdateRequest(Map<String, Object> params) {
        // Provider state setup for valid product update request
    }

    @State("product create request - name blank")
    void productCreateRequestNameBlank(Map<String, Object> params) {
        // Provider state setup for product create request with blank name
    }

    @State("product create request - name null")
    void productCreateRequestNameNull(Map<String, Object> params) {
        // Provider state setup for product create request with null name
    }

    @State("product create request - name too long")
    void productCreateRequestNameTooLong(Map<String, Object> params) {
        // Provider state setup for product create request with name too long
    }

    @State("product create request - description too long")
    void productCreateRequestDescriptionTooLong(Map<String, Object> params) {
        // Provider state setup for product create request with description too long
    }

    @State("product create request - price null")
    void productCreateRequestPriceNull(Map<String, Object> params) {
        // Provider state setup for product create request with null price
    }

    @State("product create request - price not positive")
    void productCreateRequestPriceNotPositive(Map<String, Object> params) {
        // Provider state setup for product create request with price not positive
    }

    @State("product create request - price negative")
    void productCreateRequestPriceNegative(Map<String, Object> params) {
        // Provider state setup for product create request with negative price
    }

    @State("product update request - name blank")
    void productUpdateRequestNameBlank(Map<String, Object> params) {
        // Provider state setup for product update request with blank name
    }

    @State("product update request - name null")
    void productUpdateRequestNameNull(Map<String, Object> params) {
        // Provider state setup for product update request with null name
    }

    @State("product update request - name too long")
    void productUpdateRequestNameTooLong(Map<String, Object> params) {
        // Provider state setup for product update request with name too long
    }

    @State("product update request - description too long")
    void productUpdateRequestDescriptionTooLong(Map<String, Object> params) {
        // Provider state setup for product update request with description too long
    }

    @State("product update request - price null")
    void productUpdateRequestPriceNull(Map<String, Object> params) {
        // Provider state setup for product update request with null price
    }

    @State("product update request - price not positive")
    void productUpdateRequestPriceNotPositive(Map<String, Object> params) {
        // Provider state setup for product update request with price not positive
    }
}