package com.example.order;

import com.example.order.outbox.R2dbcOutboxEventPublisher;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

/**
 * Minimal test configuration that only includes order-service beans, avoiding common module's JPA classes.
 */
@Configuration
@EnableAutoConfiguration(exclude = {
    org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
    org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration.class
})
@ComponentScan(basePackages = "com.example.order", excludeFilters = {
    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        com.example.common.event.OutboxEventPublisher.class,
        com.example.common.event.OutboxEventRepository.class,
        com.example.common.event.IdempotentEventProcessor.class
    })
})
public class MinimalTestConfig {
    @Bean
    public R2dbcOutboxEventPublisher r2dbcOutboxEventPublisher() {
        return Mockito.mock(R2dbcOutboxEventPublisher.class);
    }
}