package com.example.category.pact;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junit.State;
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
class CategoryPactProviderTest {

    @LocalServerPort
    private int port;

    private static ProviderInfo providerInfo;

    @BeforeAll
    static void setupProvider() {
        providerInfo = new ProviderInfo("category-service");
        providerInfo.setProtocol("http");
        providerInfo.setHost("localhost");
        providerInfo.setPath("/");
        
        providerInfo.hasPactWith("product-service", consumer -> {
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

    @State("valid category create request")
    void validCategoryCreateRequest(PactDslJsonBody body) {
        body.stringType("name", "Electronics")
            .stringType("description", "Electronic devices and accessories");
    }

    @State("valid category update request")
    void validCategoryUpdateRequest(PactDslJsonBody body) {
        body.stringType("name", "Consumer Electronics")
            .stringType("description", "Consumer electronic devices");
    }

    @State("category create request - name blank")
    void categoryCreateRequestNameBlank(PactDslJsonBody body) {
        body.stringValue("name", "")
            .stringType("description", "Electronic devices");
    }

    @State("category create request - name null")
    void categoryCreateRequestNameNull(PactDslJsonBody body) {
        body.stringType("name", null)
            .stringType("description", "Electronic devices");
    }

    @State("category create request - name too long")
    void categoryCreateRequestNameTooLong(PactDslJsonBody body) {
        body.stringValue("name", "A".repeat(256))
            .stringType("description", "Electronic devices");
    }

    @State("category create request - description too long")
    void categoryCreateRequestDescriptionTooLong(PactDslJsonBody body) {
        body.stringType("name", "Electronics")
            .stringValue("description", "A".repeat(1001));
    }

    @State("category update request - name blank")
    void categoryUpdateRequestNameBlank(PactDslJsonBody body) {
        body.stringValue("name", "")
            .stringType("description", "Electronic devices");
    }

    @State("category update request - name null")
    void categoryUpdateRequestNameNull(PactDslJsonBody body) {
        body.stringType("name", null)
            .stringType("description", "Electronic devices");
    }

    @State("category update request - name too long")
    void categoryUpdateRequestNameTooLong(PactDslJsonBody body) {
        body.stringValue("name", "A".repeat(256))
            .stringType("description", "Electronic devices");
    }

    @State("category update request - description too long")
    void categoryUpdateRequestDescriptionTooLong(PactDslJsonBody body) {
        body.stringType("name", "Electronics")
            .stringValue("description", "A".repeat(1001));
    }
}