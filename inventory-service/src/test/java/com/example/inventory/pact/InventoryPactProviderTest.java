package com.example.inventory.pact;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.ProviderInfo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.util.Map;

import static kotlin.Unit.INSTANCE;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(PactVerificationInvocationContextProvider.class)
@Provider("inventory-service")
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
            consumer.setPactSource(new File("../order-service/target/pacts"));
            return kotlin.Unit.INSTANCE;
        });
    }

    @LocalServerPort
    private int port;

    @BeforeEach
    void before(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
        context.setProviderInfo(providerInfo);
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("valid reserve stock request")
    void validReserveStockRequest(Map<String, Object> params) {
    }

    @State("valid confirm stock request")
    void validConfirmStockRequest(Map<String, Object> params) {
    }

    @State("reserve stock request - variantId null")
    void reserveStockRequestVariantIdNull(Map<String, Object> params) {
    }

    @State("reserve stock request - quantity null")
    void reserveStockRequestQuantityNull(Map<String, Object> params) {
    }

    @State("reserve stock request - quantity less than 1")
    void reserveStockRequestQuantityLessThanOne(Map<String, Object> params) {
    }

    @State("reserve stock request - orderItemId null")
    void reserveStockRequestOrderItemIdNull(Map<String, Object> params) {
    }

    @State("confirm stock request - variantId null")
    void confirmStockRequestVariantIdNull(Map<String, Object> params) {
    }

    @State("confirm stock request - quantity null")
    void confirmStockRequestQuantityNull(Map<String, Object> params) {
    }

    @State("confirm stock request - quantity less than 1")
    void confirmStockRequestQuantityLessThanOne(Map<String, Object> params) {
    }
}