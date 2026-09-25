package com.example.payment.config;

import com.example.payment.controller.PaymentController;
import com.example.payment.repository.PaymentRepository;
import com.example.payment.repository.PaymentEventRepository;
import com.example.payment.service.PaymentService;
import com.example.payment.event.PaymentEventPublisher;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.Optional;

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
            PaymentEventRepository paymentEventRepository,
            PaymentEventPublisher paymentEventPublisher) {
        return new PaymentService(paymentRepository, paymentEventRepository, paymentEventPublisher);
    }

    @Bean
    public PaymentEventPublisher paymentEventPublisher() {
        return new PaymentEventPublisher() {
            @Override
            public reactor.core.publisher.Mono<Void> publishPaymentEvent(Object event) {
                return reactor.core.publisher.Mono.empty();
            }
        };
    }
}
