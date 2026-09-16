package com.example.product.pact;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.junitsupport.State;
import com.example.product.CategoryClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

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

    @MockBean
    private CategoryClient categoryClient;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongo::getConnectionString);
    }

    @BeforeEach
    void before(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
        Mockito.reset(categoryClient);
        Mockito.when(categoryClient.getCategoryName(123L)).thenReturn(Optional.of("Electronics"));
    }

    @TestTemplate
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("valid product create request")
    void validProductCreateRequest() {
    }

    @State("valid product update request")
    void validProductUpdateRequest() {
    }

    @State("product create request - name blank")
    void productCreateRequestNameBlank() {
    }

    @State("product create request - name null")
    void productCreateRequestNameNull() {
    }

    @State("product create request - name too long")
    void productCreateRequestNameTooLong() {
    }

    @State("product create request - description too long")
    void productCreateRequestDescriptionTooLong() {
    }

    @State("product create request - price null")
    void productCreateRequestPriceNull() {
    }

    @State("product create request - price not positive")
    void productCreateRequestPriceNotPositive() {
    }

    @State("product create request - price negative")
    void productCreateRequestPriceNegative() {
    }

    @State("product update request - name blank")
    void productUpdateRequestNameBlank() {
    }

    @State("product update request - name null")
    void productUpdateRequestNameNull() {
    }

    @State("product update request - name too long")
    void productUpdateRequestNameTooLong() {
    }

    @State("product update request - description too long")
    void productUpdateRequestDescriptionTooLong() {
    }

    @State("product update request - price null")
    void productUpdateRequestPriceNull() {
    }

    @State("product update request - price not positive")
    void productUpdateRequestPriceNotPositive() {
    }
}