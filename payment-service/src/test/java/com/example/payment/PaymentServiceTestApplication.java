package com.example.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(exclude = {
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
    org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
})
// Only scan payment package (main and test) to avoid Servlet security config from common
@ComponentScan(basePackages = {"com.example.payment"})
@EnableDiscoveryClient
@EnableR2dbcRepositories(basePackages = {"com.example.payment.repository", "com.example.payment.event"})
@EnableTransactionManagement
public class PaymentServiceTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceTestApplication.class, args);
    }

    public static SpringApplicationBuilder createSpringApplicationBuilder() {
        return new SpringApplicationBuilder(PaymentServiceTestApplication.class)
                .web(WebApplicationType.REACTIVE)
                .properties(
                    "spring.autoconfigure.exclude="
                        + "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration,"
                        + "org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration"
                );
    }
}
