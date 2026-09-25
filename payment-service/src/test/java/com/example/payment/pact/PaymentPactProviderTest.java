package com.example.payment.pact;

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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;
import java.util.Properties;

import java.util.Map;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = com.example.payment.config.PaymentPactTestConfig.class,
    properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration,org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration,org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration,org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration,org.springframework.boot.autoconfigure.security.oauth2.resource.reactive.ReactiveOAuth2ResourceServerAutoConfiguration,org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration",
        "spring.main.web-application-type=reactive",
        "spring.security.oauth2.resourceserver.enabled=false",
        "management.security.enabled=false"
    })
@ActiveProfiles("test")
@ExtendWith(PactVerificationInvocationContextProvider.class)
@Provider("payment-service")
@PactFolder("target/pacts")
@ContextConfiguration(initializers = PaymentPactProviderTest.SecurityExclusionInitializer.class)
class PaymentPactProviderTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void before(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
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

    // Debug: print security filter chains
    @State("debug security")
    void debugSecurity(Map<String, Object> params) {
    }

    static final class SecurityExclusionInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            applicationContext.getEnvironment().getPropertySources().addFirst(
                new PropertiesPropertySource("securityExclusions", 
                    new Properties() {{
                        put("spring.autoconfigure.exclude", 
                            "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration," +
                            "org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration," +
                            "org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration," +
                            "org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration," +
                            "org.springframework.boot.autoconfigure.security.oauth2.resource.reactive.ReactiveOAuth2ResourceServerAutoConfiguration," +
                            "org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration");
                        put("spring.main.web-application-type", "reactive");
                        put("spring.security.enabled", "false");
                        put("management.security.enabled", "false");
                    }}));
        }
    }
}
