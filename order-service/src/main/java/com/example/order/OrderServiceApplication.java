package com.example.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

import com.example.common.event.OutboxPublisherAutoConfiguration;

@SpringBootApplication(exclude = {
    SecurityAutoConfiguration.class,
    OAuth2ResourceServerAutoConfiguration.class,
    JpaRepositoriesAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class,
    org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration.class
})
@Import(OutboxPublisherAutoConfiguration.class)
@ComponentScan(basePackages = {
    "com.example.order",
    "com.example.common.dto",
    "com.example.common.exception",
    "com.example.common.util"
}, excludeFilters = {
    @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.example.order.*TestConfig"),
    @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.example.order.BaseIntegrationTest.*"),
    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        com.example.common.event.OutboxEventRepository.class,
        com.example.common.event.ReactiveOutboxEventPublisher.class,
        com.example.common.event.IdempotentEventProcessor.class
    })
})
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
