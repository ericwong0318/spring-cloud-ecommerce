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
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.example.payment.config.TestSecurityConfig;
import com.example.payment.controller.PaymentController;
import com.example.payment.config.PaymentPactTestConfig;
import com.example.payment.repository.PaymentRepository;
import com.example.payment.repository.ProcessedEventRepository;
import com.example.payment.gateway.MockPaymentGateway;
import com.example.payment.gateway.GatewayResponse;
import com.example.payment.domain.Payment;
import com.example.payment.domain.Payment.PaymentStatus;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import java.util.Map;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = com.example.payment.config.PaymentPactTestConfig.class,
    properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration,org.springframework.boot.actuate.autoconfig.security.servlet.ManagementWebSecurityAutoConfiguration,org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration,org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration,org.springframework.boot.autoconfigure.security.oauth2.resource.reactive.ReactiveOAuth2ResourceServerAutoConfiguration,org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration",
        "spring.main.web-application-type=reactive",
        "spring.security.oauth2.resourceserver.enabled=false",
        "management.security.enabled=false"
    })
@ActiveProfiles("test")
@ExtendWith(PactVerificationInvocationContextProvider.class)
@Provider("payment-service")
@PactFolder("target/pacts")
@Import({TestSecurityConfig.class, PaymentController.class})
class PaymentPactProviderTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private com.example.payment.gateway.MockPaymentGateway mockPaymentGateway;

    @Autowired
    private com.example.payment.event.PaymentEventPublisher paymentEventPublisher;

    @Autowired
    private org.springframework.transaction.reactive.TransactionalOperator transactionalOperator;

    @LocalServerPort
    private int port;

    @BeforeEach
    void before(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
        Mockito.reset(paymentRepository, mockPaymentGateway, paymentEventPublisher, transactionalOperator);
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("valid authorize request")
    void validAuthorizeRequest(Map<String, Object> params) {
        Mockito.when(paymentRepository.findByIdempotencyKey(Mockito.anyString()))
            .thenReturn(reactor.core.publisher.Mono.empty());
        
        Mockito.when(mockPaymentGateway.authorize(
            Mockito.any(), Mockito.any(), Mockito.anyString(), Mockito.any()))
            .thenReturn(reactor.core.publisher.Mono.just(new com.example.payment.gateway.GatewayResponse(true, "txn-123", "SUCCESS", null)));
        
        Mockito.when(paymentRepository.save(Mockito.any()))
            .thenAnswer(invocation -> {
                com.example.payment.domain.Payment p = invocation.getArgument(0);
                if (p.id() == null) {
                    return reactor.core.publisher.Mono.just(new com.example.payment.domain.Payment(
                        1L, p.orderId(), p.amount(), p.currency(), p.idempotencyKey(),
                        "txn-123", com.example.payment.domain.Payment.PaymentStatus.AUTHORIZED, p.customerId(), p.customerEmail(),
                        java.time.LocalDateTime.now(), null, null, java.time.LocalDateTime.now(), java.time.LocalDateTime.now()));
                }
                return reactor.core.publisher.Mono.just(p);
            });
        
        Mockito.when(paymentEventPublisher.publish(Mockito.any()))
            .thenReturn(reactor.core.publisher.Mono.empty());
        
        Mockito.when(transactionalOperator.transactional(Mockito.any()))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @State("valid capture request")
    void validCaptureRequest(Map<String, Object> params) {
        com.example.payment.domain.Payment authorizedPayment = new com.example.payment.domain.Payment(
            1L, 100L, new java.math.BigDecimal("100.00"), "USD", "idem-123",
            "txn-123", com.example.payment.domain.Payment.PaymentStatus.AUTHORIZED, "cust-1", "test@example.com",
            java.time.LocalDateTime.now(), null, null, java.time.LocalDateTime.now(), java.time.LocalDateTime.now());
        
        Mockito.when(paymentRepository.findById(1L))
            .thenReturn(reactor.core.publisher.Mono.just(authorizedPayment));
        
        Mockito.when(mockPaymentGateway.capture(Mockito.anyString()))
            .thenReturn(reactor.core.publisher.Mono.just(new com.example.payment.gateway.GatewayResponse(true, "txn-123", "CAPTURED", null)));
        
        Mockito.when(paymentRepository.save(Mockito.any()))
            .thenAnswer(invocation -> reactor.core.publisher.Mono.just(invocation.getArgument(0)));
        
        Mockito.when(transactionalOperator.transactional(Mockito.any()))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @State("valid refund request")
    void validRefundRequest(Map<String, Object> params) {
        com.example.payment.domain.Payment capturedPayment = new com.example.payment.domain.Payment(
            1L, 100L, new java.math.BigDecimal("100.00"), "USD", "idem-123",
            "txn-123", com.example.payment.domain.Payment.PaymentStatus.CAPTURED, "cust-1", "test@example.com",
            java.time.LocalDateTime.now(), java.time.LocalDateTime.now(), null, java.time.LocalDateTime.now(), java.time.LocalDateTime.now());
        
        Mockito.when(paymentRepository.findById(1L))
            .thenReturn(reactor.core.publisher.Mono.just(capturedPayment));
        
        Mockito.when(mockPaymentGateway.refund(Mockito.anyString(), Mockito.any()))
            .thenReturn(reactor.core.publisher.Mono.just(new com.example.payment.gateway.GatewayResponse(true, "refund-123", "REFUNDED", null)));
        
        Mockito.when(paymentRepository.save(Mockito.any()))
            .thenAnswer(invocation -> reactor.core.publisher.Mono.just(invocation.getArgument(0)));
        
        Mockito.when(transactionalOperator.transactional(Mockito.any()))
            .thenAnswer(invocation -> invocation.getArgument(0));
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
