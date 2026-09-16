package com.example.payment.pact;

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
import java.math.BigDecimal;
import java.util.Map;

import static kotlin.Unit.INSTANCE;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(PactVerificationInvocationContextProvider.class)
@Provider("payment-service")
class PaymentPactProviderTest {

    @LocalServerPort
    private int port;

    private static ProviderInfo providerInfo;

    @BeforeAll
    static void setupProvider() {
        providerInfo = new ProviderInfo("payment-service");
        providerInfo.setProtocol("http");
        providerInfo.setHost("localhost");
        providerInfo.setPath("/");

        providerInfo.hasPactWith("order-service", consumer -> {
            consumer.setPactSource(new File("../order-service/target/pacts"));
            return kotlin.Unit.INSTANCE;
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

    @State("valid authorize request")
    void validAuthorizeRequest(Map<String, Object> params) {
    }

    @State("valid capture request")
    void validCaptureRequest(Map<String, Object> params) {
    }

    @State("valid refund request")
    void validRefundRequest(Map<String, Object> params) {
    }

    @State("authorize request - orderId null")
    void authorizeRequestOrderIdNull(Map<String, Object> params) {
    }

    @State("authorize request - amount null")
    void authorizeRequestAmountNull(Map<String, Object> params) {
    }

    @State("authorize request - currency blank")
    void authorizeRequestCurrencyBlank(Map<String, Object> params) {
    }

    @State("authorize request - currency invalid length")
    void authorizeRequestCurrencyInvalidLength(Map<String, Object> params) {
    }

    @State("authorize request - customerId blank")
    void authorizeRequestCustomerIdBlank(Map<String, Object> params) {
    }

    @State("authorize request - customerId too long")
    void authorizeRequestCustomerIdTooLong(Map<String, Object> params) {
    }

    @State("authorize request - customerEmail blank")
    void authorizeRequestCustomerEmailBlank(Map<String, Object> params) {
    }

    @State("authorize request - customerEmail too long")
    void authorizeRequestCustomerEmailTooLong(Map<String, Object> params) {
    }

    @State("authorize request - idempotencyKey blank")
    void authorizeRequestIdempotencyKeyBlank(Map<String, Object> params) {
    }

    @State("authorize request - idempotencyKey too long")
    void authorizeRequestIdempotencyKeyTooLong(Map<String, Object> params) {
    }

    @State("capture request - gatewayTransactionId blank")
    void captureRequestGatewayTransactionIdBlank(Map<String, Object> params) {
    }

    @State("capture request - gatewayTransactionId too long")
    void captureRequestGatewayTransactionIdTooLong(Map<String, Object> params) {
    }

    @State("refund request - amount null")
    void refundRequestAmountNull(Map<String, Object> params) {
    }

    @State("refund request - reason too long")
    void refundRequestReasonTooLong(Map<String, Object> params) {
    }
}