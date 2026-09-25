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
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.example.payment.config.TestSecurityConfig;
import com.example.payment.controller.PaymentController;
import com.example.payment.config.PaymentPactTestConfig;
import com.example.payment.repository.PaymentRepository;
import com.example.payment.repository.ProcessedEventRepository;
import com.example.payment.gateway.MockPaymentGateway;
import com.example.payment.gateway.MockPaymentGateway.GatewayResponse;
import com.example.payment.domain.Payment;
import com.example.payment.domain.Payment.PaymentStatus;
import com.example.payment.event.PaymentEventPublisher;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
@ControllerAdvice
class PaymentPactProviderTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private MockPaymentGateway mockPaymentGateway;

    @Autowired
    private PaymentEventPublisher paymentEventPublisher;

    @Autowired
    private TransactionalOperator transactionalOperator;

    @LocalServerPort
    private int port;

    @BeforeEach
    void before(PactVerificationContext context) {
        Mockito.reset(paymentRepository, mockPaymentGateway, paymentEventPublisher, transactionalOperator);
        context.setTarget(new HttpTestTarget("localhost", port));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @ExceptionHandler(org.springframework.web.bind.support.WebExchangeBindException.class)
    public org.springframework.http.ResponseEntity<java.util.Map<String, Object>> handleValidationExceptions(
            org.springframework.web.bind.support.WebExchangeBindException ex) {
        java.util.Map<String, Object> errors = new java.util.HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((org.springframework.validation.FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        java.util.Map<String, Object> problem = new java.util.HashMap<>();
        problem.put("type", "https://api.example.com/errors/validation");
        problem.put("title", "Validation Failed");
        problem.put("status", org.springframework.http.HttpStatus.BAD_REQUEST.value());
        problem.put("detail", "Invalid request parameters");
        problem.put("timestamp", java.time.Instant.now());
        problem.put("errors", errors);

        return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST).body(problem);
    }

    @State("valid authorize request")
    void validAuthorizeRequest(java.util.Map<String, Object> params) {
        Mockito.when(paymentRepository.findByIdempotencyKey("idem-key-123"))
            .thenReturn(reactor.core.publisher.Mono.empty());

        Mockito.when(mockPaymentGateway.authorize(
                Mockito.any(), Mockito.any(), Mockito.eq("idem-key-123"), Mockito.isNull()))
            .thenReturn(reactor.core.publisher.Mono.just(new com.example.payment.gateway.MockPaymentGateway.GatewayResponse(true, "APPROVED", "Mock authorization successful", "txn-abc-123")));

        Mockito.when(paymentRepository.save(Mockito.any(com.example.payment.domain.Payment.class)))
            .thenAnswer(invocation -> {
                com.example.payment.domain.Payment p = invocation.getArgument(0);
                if (p.id() == null) {
                    return reactor.core.publisher.Mono.just(new com.example.payment.domain.Payment(
                        1L, p.orderId(), p.amount(), p.currency(), p.status(),
                        "txn-abc-123", p.idempotencyKey(),
                        p.authorizedAt(), 
                        LocalDateTime.now(),  // capturedAt - use a date instead of null
                        LocalDateTime.now(),  // refundedAt - use a date instead of null
                        p.createdAt(), p.updatedAt()));
                }
                return reactor.core.publisher.Mono.just(p);
            });

        Mockito.when(paymentEventPublisher.publish(Mockito.any()))
            .thenReturn(reactor.core.publisher.Mono.empty());

        Mockito.when(transactionalOperator.transactional(Mockito.any(reactor.core.publisher.Mono.class)))
            .thenAnswer(inv -> inv.getArgument(0));
    }

    @State("valid capture request")
    void validCaptureRequest(java.util.Map<String, Object> params) {
        com.example.payment.domain.Payment authorizedPayment = new com.example.payment.domain.Payment(
            1L, 100L, new BigDecimal("100.00"), "USD", com.example.payment.domain.Payment.PaymentStatus.AUTHORIZED,
            "txn-abc-123", "idem-key-123",
            LocalDateTime.now(), null, null,
            LocalDateTime.now(), LocalDateTime.now());

        Mockito.when(paymentRepository.findById(1L))
            .thenReturn(reactor.core.publisher.Mono.just(authorizedPayment));

        Mockito.when(mockPaymentGateway.capture(Mockito.anyString(), Mockito.isNull()))
            .thenReturn(reactor.core.publisher.Mono.just(new com.example.payment.gateway.MockPaymentGateway.GatewayResponse(true, "CAPTURED", "Mock capture successful", "txn-abc-123")));

        Mockito.when(paymentRepository.save(Mockito.any(com.example.payment.domain.Payment.class)))
            .thenAnswer(inv -> {
                com.example.payment.domain.Payment p = inv.getArgument(0);
                return reactor.core.publisher.Mono.just(new com.example.payment.domain.Payment(
                    p.id(), p.orderId(), p.amount(), p.currency(), p.status(),
                    p.gatewayTransactionId(), p.idempotencyKey(),
                    p.authorizedAt(), p.capturedAt(),
                    LocalDateTime.now(),  // refundedAt - use a date instead of null
                    p.createdAt(), p.updatedAt()));
            });

        Mockito.when(paymentEventPublisher.publish(Mockito.any()))
            .thenReturn(reactor.core.publisher.Mono.empty());

        Mockito.when(transactionalOperator.transactional(Mockito.any(reactor.core.publisher.Mono.class)))
            .thenAnswer(inv -> inv.getArgument(0));
    }

    @State("valid refund request")
    void validRefundRequest(java.util.Map<String, Object> params) {
        com.example.payment.domain.Payment capturedPayment = new com.example.payment.domain.Payment(
            1L, 100L, new BigDecimal("100.00"), "USD", com.example.payment.domain.Payment.PaymentStatus.CAPTURED,
            "txn-abc-123", "idem-key-123",
            LocalDateTime.now(), LocalDateTime.now(), null,
            LocalDateTime.now(), LocalDateTime.now());

        Mockito.when(paymentRepository.findById(1L))
            .thenReturn(reactor.core.publisher.Mono.just(capturedPayment));

        Mockito.when(paymentRepository.findByIdempotencyKey(Mockito.anyString()))
            .thenReturn(reactor.core.publisher.Mono.empty());

        Mockito.when(mockPaymentGateway.refund(Mockito.anyString(), Mockito.any(BigDecimal.class), Mockito.isNull()))
            .thenReturn(reactor.core.publisher.Mono.just(new com.example.payment.gateway.MockPaymentGateway.GatewayResponse(true, "REFUNDED", "Mock refund successful", "refund-123")));

        Mockito.when(paymentRepository.save(Mockito.any(com.example.payment.domain.Payment.class)))
            .thenAnswer(inv -> {
                com.example.payment.domain.Payment p = inv.getArgument(0);
                return reactor.core.publisher.Mono.just(new com.example.payment.domain.Payment(
                    p.id(), p.orderId(), p.amount(), p.currency(), p.status(),
                    p.gatewayTransactionId(), p.idempotencyKey(),
                    p.authorizedAt(), p.capturedAt(),
                    p.refundedAt(),  // keep the refundedAt from the saved payment
                    p.createdAt(), p.updatedAt()));
            });

        Mockito.when(paymentEventPublisher.publish(Mockito.any()))
            .thenReturn(reactor.core.publisher.Mono.empty());

        Mockito.when(transactionalOperator.transactional(Mockito.any(reactor.core.publisher.Mono.class)))
            .thenAnswer(inv -> inv.getArgument(0));
    }

    @State("authorize request - orderId null")
    void authorizeRequestOrderIdNull(java.util.Map<String, Object> params) {
        Mockito.when(paymentRepository.findByIdempotencyKey(Mockito.anyString()))
            .thenReturn(reactor.core.publisher.Mono.empty());
        Mockito.when(transactionalOperator.transactional(Mockito.any(reactor.core.publisher.Mono.class)))
            .thenAnswer(inv -> inv.getArgument(0));
    }

    @State("authorize request - amount null")
    void authorizeRequestAmountNull(java.util.Map<String, Object> params) {
        Mockito.when(paymentRepository.findByIdempotencyKey(Mockito.anyString()))
            .thenReturn(reactor.core.publisher.Mono.empty());
        Mockito.when(transactionalOperator.transactional(Mockito.any(reactor.core.publisher.Mono.class)))
            .thenAnswer(inv -> inv.getArgument(0));
    }

    @State("authorize request - currency blank")
    void authorizeRequestCurrencyBlank(java.util.Map<String, Object> params) {
        Mockito.when(paymentRepository.findByIdempotencyKey(Mockito.anyString()))
            .thenReturn(reactor.core.publisher.Mono.empty());
        Mockito.when(transactionalOperator.transactional(Mockito.any(reactor.core.publisher.Mono.class)))
            .thenAnswer(inv -> inv.getArgument(0));
    }

    @State("authorize request - currency invalid length")
    void authorizeRequestCurrencyInvalidLength(java.util.Map<String, Object> params) {
        Mockito.when(paymentRepository.findByIdempotencyKey(Mockito.anyString()))
            .thenReturn(reactor.core.publisher.Mono.empty());
        Mockito.when(transactionalOperator.transactional(Mockito.any(reactor.core.publisher.Mono.class)))
            .thenAnswer(inv -> inv.getArgument(0));
    }

    @State("authorize request - customerId blank")
    void authorizeRequestCustomerIdBlank(java.util.Map<String, Object> params) {
        Mockito.when(paymentRepository.findByIdempotencyKey(Mockito.anyString()))
            .thenReturn(reactor.core.publisher.Mono.empty());
        Mockito.when(transactionalOperator.transactional(Mockito.any(reactor.core.publisher.Mono.class)))
            .thenAnswer(inv -> inv.getArgument(0));
    }

    @State("authorize request - customerId too long")
    void authorizeRequestCustomerIdTooLong(java.util.Map<String, Object> params) {
        Mockito.when(paymentRepository.findByIdempotencyKey(Mockito.anyString()))
            .thenReturn(reactor.core.publisher.Mono.empty());
        Mockito.when(transactionalOperator.transactional(Mockito.any(reactor.core.publisher.Mono.class)))
            .thenAnswer(inv -> inv.getArgument(0));
    }

    @State("authorize request - customerEmail blank")
    void authorizeRequestCustomerEmailBlank(java.util.Map<String, Object> params) {
        Mockito.when(paymentRepository.findByIdempotencyKey(Mockito.anyString()))
            .thenReturn(reactor.core.publisher.Mono.empty());
        Mockito.when(transactionalOperator.transactional(Mockito.any(reactor.core.publisher.Mono.class)))
            .thenAnswer(inv -> inv.getArgument(0));
    }

    @State("authorize request - customerEmail too long")
    void authorizeRequestCustomerEmailTooLong(java.util.Map<String, Object> params) {
        Mockito.when(paymentRepository.findByIdempotencyKey(Mockito.anyString()))
            .thenReturn(reactor.core.publisher.Mono.empty());
        Mockito.when(transactionalOperator.transactional(Mockito.any(reactor.core.publisher.Mono.class)))
            .thenAnswer(inv -> inv.getArgument(0));
    }

    @State("authorize request - idempotencyKey blank")
    void authorizeRequestIdempotencyKeyBlank(java.util.Map<String, Object> params) {
        Mockito.when(paymentRepository.findByIdempotencyKey(Mockito.anyString()))
            .thenReturn(reactor.core.publisher.Mono.empty());
        Mockito.when(transactionalOperator.transactional(Mockito.any(reactor.core.publisher.Mono.class)))
            .thenAnswer(inv -> inv.getArgument(0));
    }

    @State("authorize request - idempotencyKey too long")
    void authorizeRequestIdempotencyKeyTooLong(java.util.Map<String, Object> params) {
        Mockito.when(paymentRepository.findByIdempotencyKey(Mockito.anyString()))
            .thenReturn(reactor.core.publisher.Mono.empty());
        Mockito.when(transactionalOperator.transactional(Mockito.any(reactor.core.publisher.Mono.class)))
            .thenAnswer(inv -> inv.getArgument(0));
    }

    @State("capture request - gatewayTransactionId blank")
    void captureRequestGatewayTransactionIdBlank(java.util.Map<String, Object> params) {
        Mockito.when(paymentRepository.findById(Mockito.anyLong()))
            .thenReturn(reactor.core.publisher.Mono.empty());
        Mockito.when(transactionalOperator.transactional(Mockito.any(reactor.core.publisher.Mono.class)))
            .thenAnswer(inv -> inv.getArgument(0));
    }

    @State("capture request - gatewayTransactionId too long")
    void captureRequestGatewayTransactionIdTooLong(java.util.Map<String, Object> params) {
        Mockito.when(paymentRepository.findById(Mockito.anyLong()))
            .thenReturn(reactor.core.publisher.Mono.empty());
        Mockito.when(transactionalOperator.transactional(Mockito.any(reactor.core.publisher.Mono.class)))
            .thenAnswer(inv -> inv.getArgument(0));
    }

    @State("refund request - amount null")
    void refundRequestAmountNull(java.util.Map<String, Object> params) {
        Mockito.when(paymentRepository.findById(Mockito.anyLong()))
            .thenReturn(reactor.core.publisher.Mono.empty());
        Mockito.when(transactionalOperator.transactional(Mockito.any(reactor.core.publisher.Mono.class)))
            .thenAnswer(inv -> inv.getArgument(0));
    }

    @State("refund request - reason too long")
    void refundRequestReasonTooLong(java.util.Map<String, Object> params) {
        Mockito.when(paymentRepository.findById(Mockito.anyLong()))
            .thenReturn(reactor.core.publisher.Mono.empty());
        Mockito.when(transactionalOperator.transactional(Mockito.any(reactor.core.publisher.Mono.class)))
            .thenAnswer(inv -> inv.getArgument(0));
    }
}
