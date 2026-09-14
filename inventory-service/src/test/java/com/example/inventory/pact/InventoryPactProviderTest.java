package com.example.inventory.pact;

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
class InventoryPactProviderTest {

    @LocalServerPort
    private int port;

    private static ProviderInfo providerInfo;

    @BeforeAll
    static void setupProvider() {
        providerInfo = new ProviderInfo("inventory-service");
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

    @State("valid reserve stock request")
    void validReserveStockRequest(PactDslJsonBody body) {
        body.integerType("variantId", 1L)
            .integerType("quantity", 5)
            .integerType("orderItemId", 100L);
    }

    @State("valid confirm stock request")
    void validConfirmStockRequest(PactDslJsonBody body) {
        body.integerType("variantId", 1L)
            .integerType("quantity", 5);
    }

    @State("reserve stock request - variantId null")
    void reserveStockRequestVariantIdNull(PactDslJsonBody body) {
        body.stringType("variantId", null)
            .integerType("quantity", 5)
            .integerType("orderItemId", 100L);
    }

    @State("reserve stock request - quantity null")
    void reserveStockRequestQuantityNull(PactDslJsonBody body) {
        body.integerType("variantId", 1L)
            .stringType("quantity", null)
            .integerType("orderItemId", 100L);
    }

    @State("reserve stock request - quantity less than 1")
    void reserveStockRequestQuantityLessThanOne(PactDslJsonBody body) {
        body.integerType("variantId", 1L)
            .integerType("quantity", 0)
            .integerType("orderItemId", 100L);
    }

    @State("reserve stock request - orderItemId null")
    void reserveStockRequestOrderItemIdNull(PactDslJsonBody body) {
        body.integerType("variantId", 1L)
            .integerType("quantity", 5)
            .stringType("orderItemId", null);
    }

    @State("confirm stock request - variantId null")
    void confirmStockRequestVariantIdNull(PactDslJsonBody body) {
        body.stringType("variantId", null)
            .integerType("quantity", 5);
    }

    @State("confirm stock request - quantity null")
    void confirmStockRequestQuantityNull(PactDslJsonBody body) {
        body.integerType("variantId", 1L)
            .stringType("quantity", null);
    }

    @State("confirm stock request - quantity less than 1")
    void confirmStockRequestQuantityLessThanOne(PactDslJsonBody body) {
        body.integerType("variantId", 1L)
            .integerType("quantity", 0);
    }
}