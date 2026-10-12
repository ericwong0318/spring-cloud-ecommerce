package com.example.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.web.reactive.config.EnableWebFlux;

/**
 * Test-specific Spring Boot application that excludes problematic components.
 */
@EnableWebFlux
@SpringBootApplication(exclude = {
    SecurityAutoConfiguration.class,
    OAuth2ResourceServerAutoConfiguration.class,
    JpaRepositoriesAutoConfiguration.class
})
@ComponentScan(basePackages = {
    "com.example.order",
    "com.example.common.dto",
    "com.example.common.exception",
    "com.example.common.util"
}, excludeFilters = {
    @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.example.order.*TestConfig"),
    @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.example.order.*PactProviderTest.*"),
    @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.example.order.*PactConsumerTest.*"),
    @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.example.order.*PactTestConfig.*"),
    @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.example.order.outbox.*")
})
public class TestOrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestOrderServiceApplication.class, args);
    }
}