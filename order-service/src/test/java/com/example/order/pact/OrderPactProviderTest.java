package com.example.order.pact;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.ProviderInfo;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(PactVerificationInvocationContextProvider.class)
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

        providerInfo.hasPactWith("product", consumer -> {
            consumer.setPactSource("target/pacts");
            return kotlin.Unit.INSTANCE;
        });
        providerInfo.hasPactWith("payment-service", consumer -> {
            consumer.setPactSource("target/pacts");
            return kotlin.Unit.INSTANCE;
        });
        providerInfo.hasPactWith("inventory-service", consumer -> {
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

    @State("valid order create request")
    void validOrderCreateRequest(PactDslJsonBody body) {
        body.stringType("customerId", "CUST-001")
            .stringType("customerEmail", "customer@example.com")
            .decimalType("totalAmount", new BigDecimal("1999.98"))
            .array("items")
                .object()
                    .stringType("productId", "1")
                    .stringType("variantId", "1")
                    .integerType("quantity", 2)
                    .decimalType("price", new BigDecimal("999.99"))
                .closeObject()
            .closeArray();
    }

    @State("valid order update request")
    void validOrderUpdateRequest(PactDslJsonBody body) {
        body.stringType("customerId", "CUST-001")
            .stringType("customerEmail", "customer@example.com")
            .decimalType("totalAmount", new BigDecimal("2999.97"))
            .array("items")
                .object()
                    .stringType("productId", "1")
                    .stringType("variantId", "1")
                    .integerType("quantity", 3)
                    .decimalType("price", new BigDecimal("999.99"))
                .closeObject()
            .closeArray();
    }

    @State("order create request - customerId blank")
    void orderCreateRequestCustomerIdBlank(PactDslJsonBody body) {
        body.stringValue("customerId", "")
            .stringType("customerEmail", "customer@example.com")
            .decimalType("totalAmount", new BigDecimal("1999.98"))
            .array("items")
                .object()
                    .stringType("productId", "1")
                    .stringType("variantId", "1")
                    .integerType("quantity", 2)
                    .decimalType("price", new BigDecimal("999.99"))
                .closeObject()
            .closeArray();
    }

    @State("order create request - customerEmail invalid")
    void orderCreateRequestCustomerEmailInvalid(PactDslJsonBody body) {
        body.stringType("customerId", "CUST-001")
            .stringValue("customerEmail", "invalid-email")
            .decimalType("totalAmount", new BigDecimal("1999.98"))
            .array("items")
                .object()
                    .stringType("productId", "1")
                    .stringType("variantId", "1")
                    .integerType("quantity", 2)
                    .decimalType("price", new BigDecimal("999.99"))
                .closeObject()
            .closeArray();
    }

    @State("order create request - totalAmount null")
    void orderCreateRequestTotalAmountNull(PactDslJsonBody body) {
        body.stringType("customerId", "CUST-001")
            .stringType("customerEmail", "customer@example.com")
            .stringType("totalAmount", null)
            .array("items")
                .object()
                    .stringType("productId", "1")
                    .stringType("variantId", "1")
                    .integerType("quantity", 2)
                    .decimalType("price", new BigDecimal("999.99"))
                .closeObject()
            .closeArray();
    }

    @State("order create request - items empty")
    void orderCreateRequestItemsEmpty(PactDslJsonBody body) {
        body.stringType("customerId", "CUST-001")
            .stringType("customerEmail", "customer@example.com")
            .decimalType("totalAmount", new BigDecimal("1999.98"))
            .array("items")
            .closeArray();
    }
}