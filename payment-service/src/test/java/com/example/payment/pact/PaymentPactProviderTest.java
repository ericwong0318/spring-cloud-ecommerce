package com.example.payment.pact;

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

    @State("valid authorize request")
    void validAuthorizeRequest(PactDslJsonBody body) {
        body.integerType("orderId", 1L)
            .decimalType("amount", "99.99")
            .stringType("currency", "USD")
            .stringType("customerId", "cust-123")
            .stringType("customerEmail", "customer@example.com")
            .stringType("idempotencyKey", "idem-key-123");
    }

    @State("valid capture request")
    void validCaptureRequest(PactDslJsonBody body) {
        body.stringType("gatewayTransactionId", "txn-abc-123");
    }

    @State("valid refund request")
    void validRefundRequest(PactDslJsonBody body) {
        body.decimalType("amount", "50.00")
            .stringType("reason", "Customer requested refund");
    }

    @State("authorize request - orderId null")
    void authorizeRequestOrderIdNull(PactDslJsonBody body) {
        body.stringType("orderId", null)
            .decimalType("amount", "99.99")
            .stringType("currency", "USD")
            .stringType("customerId", "cust-123")
            .stringType("customerEmail", "customer@example.com")
            .stringType("idempotencyKey", "idem-key-123");
    }

    @State("authorize request - amount null")
    void authorizeRequestAmountNull(PactDslJsonBody body) {
        body.integerType("orderId", 1L)
            .stringType("amount", null)
            .stringType("currency", "USD")
            .stringType("customerId", "cust-123")
            .stringType("customerEmail", "customer@example.com")
            .stringType("idempotencyKey", "idem-key-123");
    }

    @State("authorize request - currency blank")
    void authorizeRequestCurrencyBlank(PactDslJsonBody body) {
        body.integerType("orderId", 1L)
            .decimalType("amount", "99.99")
            .stringValue("currency", "")
            .stringType("customerId", "cust-123")
            .stringType("customerEmail", "customer@example.com")
            .stringType("idempotencyKey", "idem-key-123");
    }

    @State("authorize request - currency invalid length")
    void authorizeRequestCurrencyInvalidLength(PactDslJsonBody body) {
        body.integerType("orderId", 1L)
            .decimalType("amount", "99.99")
            .stringValue("currency", "US")
            .stringType("customerId", "cust-123")
            .stringType("customerEmail", "customer@example.com")
            .stringType("idempotencyKey", "idem-key-123");
    }

    @State("authorize request - customerId blank")
    void authorizeRequestCustomerIdBlank(PactDslJsonBody body) {
        body.integerType("orderId", 1L)
            .decimalType("amount", "99.99")
            .stringType("currency", "USD")
            .stringValue("customerId", "")
            .stringType("customerEmail", "customer@example.com")
            .stringType("idempotencyKey", "idem-key-123");
    }

    @State("authorize request - customerId too long")
    void authorizeRequestCustomerIdTooLong(PactDslJsonBody body) {
        body.integerType("orderId", 1L)
            .decimalType("amount", "99.99")
            .stringType("currency", "USD")
            .stringValue("customerId", "A".repeat(256))
            .stringType("customerEmail", "customer@example.com")
            .stringType("idempotencyKey", "idem-key-123");
    }

    @State("authorize request - customerEmail blank")
    void authorizeRequestCustomerEmailBlank(PactDslJsonBody body) {
        body.integerType("orderId", 1L)
            .decimalType("amount", "99.99")
            .stringType("currency", "USD")
            .stringType("customerId", "cust-123")
            .stringValue("customerEmail", "")
            .stringType("idempotencyKey", "idem-key-123");
    }

    @State("authorize request - customerEmail too long")
    void authorizeRequestCustomerEmailTooLong(PactDslJsonBody body) {
        body.integerType("orderId", 1L)
            .decimalType("amount", "99.99")
            .stringType("currency", "USD")
            .stringType("customerId", "cust-123")
            .stringValue("customerEmail", "A".repeat(256))
            .stringType("idempotencyKey", "idem-key-123");
    }

    @State("authorize request - idempotencyKey blank")
    void authorizeRequestIdempotencyKeyBlank(PactDslJsonBody body) {
        body.integerType("orderId", 1L)
            .decimalType("amount", "99.99")
            .stringType("currency", "USD")
            .stringType("customerId", "cust-123")
            .stringType("customerEmail", "customer@example.com")
            .stringValue("idempotencyKey", "");
    }

    @State("authorize request - idempotencyKey too long")
    void authorizeRequestIdempotencyKeyTooLong(PactDslJsonBody body) {
        body.integerType("orderId", 1L)
            .decimalType("amount", "99.99")
            .stringType("currency", "USD")
            .stringType("customerId", "cust-123")
            .stringType("customerEmail", "customer@example.com")
            .stringValue("idempotencyKey", "A".repeat(101));
    }

    @State("capture request - gatewayTransactionId blank")
    void captureRequestGatewayTransactionIdBlank(PactDslJsonBody body) {
        body.stringValue("gatewayTransactionId", "");
    }

    @State("capture request - gatewayTransactionId too long")
    void captureRequestGatewayTransactionIdTooLong(PactDslJsonBody body) {
        body.stringValue("gatewayTransactionId", "A".repeat(101));
    }

    @State("refund request - amount null")
    void refundRequestAmountNull(PactDslJsonBody body) {
        body.stringType("amount", null)
            .stringType("reason", "Customer requested refund");
    }

    @State("refund request - reason too long")
    void refundRequestReasonTooLong(PactDslJsonBody body) {
        body.decimalType("amount", "50.00")
            .stringValue("reason", "A".repeat(501));
    }
}