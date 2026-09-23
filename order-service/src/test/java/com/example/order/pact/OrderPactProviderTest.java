package com.example.order.pact;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.junitsupport.State;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(PactVerificationInvocationContextProvider.class)
@Provider("order-service")
@PactFolder("target/pacts")
class OrderPactProviderTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void before(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
    }

    @TestTemplate
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