package com.example.payment.config;

import com.example.payment.controller.PaymentController;
import com.example.payment.gateway.MockPaymentGateway;
import com.example.payment.repository.PaymentRepository;
import com.example.payment.repository.ProcessedEventRepository;
import com.example.payment.service.PaymentService;
import com.example.payment.event.PaymentEventPublisher;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

@Configuration
@EnableAutoConfiguration(exclude = {
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
    org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.oauth2.resource.reactive.ReactiveOAuth2ResourceServerAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration.class
})
@Import({TestSecurityConfig.class, PaymentController.class})
public class PaymentPactTestConfig {

    @Bean
    public PaymentService paymentService(
            PaymentRepository paymentRepository,
            com.example.payment.repository.ProcessedEventRepository processedEventRepository,
            PaymentEventPublisher paymentEventPublisher,
            MockPaymentGateway mockPaymentGateway,
            TransactionalOperator transactionalOperator) {
        return new PaymentService(
            paymentRepository,
            processedEventRepository,
            paymentEventPublisher,
            mockPaymentGateway,
            transactionalOperator
        );
    }

    @Bean
    public PaymentEventPublisher paymentEventPublisher() {
        return org.mockito.Mockito.mock(PaymentEventPublisher.class);
    }

    @Bean
    public PaymentRepository paymentRepository() {
        return org.mockito.Mockito.mock(PaymentRepository.class);
    }

    @Bean
    public ProcessedEventRepository processedEventRepository() {
        return org.mockito.Mockito.mock(ProcessedEventRepository.class);
    }

    @Bean
    public MockPaymentGateway mockPaymentGateway() {
        return org.mockito.Mockito.mock(MockPaymentGateway.class);
    }

    @Bean
    public TransactionalOperator transactionalOperator() {
        return org.mockito.Mockito.mock(TransactionalOperator.class);
    }
}
