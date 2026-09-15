package com.example.order;

import com.example.order.outbox.R2dbcOutboxEventPublisher;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Minimal test configuration that only includes order-service beans, avoiding common module's JPA classes.
 */
@Configuration
public class MinimalTestConfig {
    @Bean
    public R2dbcOutboxEventPublisher r2dbcOutboxEventPublisher() {
        return Mockito.mock(R2dbcOutboxEventPublisher.class);
    }
}