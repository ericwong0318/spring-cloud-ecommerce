package com.example.order.pact;

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
@Provider("order-service")
class OrderPactProviderTest {

    @LocalServerPort
    private int port;

    private static ProviderInfo providerInfo;

    @BeforeAll
    static void setupProvider() {
        providerInfo = new ProviderInfo("order-service");
        providerInfo.setProtocol("http");
        providerInfo.setHost("localhost");
        providerInfo.setPath("/");

        providerInfo.hasPactWith("order-service", consumer -> {
            consumer.setPactSource(new File("../order-service/target/pacts"));
            return INSTANCE;
        });
        providerInfo.hasPactWith("product-service", consumer -> {
            consumer.setPactSource(new File("../product/target/pacts"));
            return INSTANCE;
        });
        providerInfo.hasPactWith("payment-service", consumer -> {
            consumer.setPactSource(new File("../payment-service/target/pacts"));
            return INSTANCE;
        });
        providerInfo.hasPactWith("inventory-service", consumer -> {
            consumer.setPactSource(new File("../inventory-service/target/pacts"));
            return INSTANCE;
        });
    }

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

    @State("valid order create request")
    void validOrderCreateRequest(Map<String, Object> params) {
    }

    @State("valid order update request")
    void validOrderUpdateRequest(Map<String, Object> params) {
    }

    @State("order create request - customerId blank")
    void orderCreateRequestCustomerIdBlank(Map<String, Object> params) {
    }

    @State("order create request - customerEmail invalid")
    void orderCreateRequestCustomerEmailInvalid(Map<String, Object> params) {
    }

    @State("order create request - totalAmount null")
    void orderCreateRequestTotalAmountNull(Map<String, Object> params) {
    }

    @State("order create request - items empty")
    void orderCreateRequestItemsEmpty(Map<String, Object> params) {
    }
}