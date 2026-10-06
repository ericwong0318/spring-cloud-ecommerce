package com.example.inventory.integration;

import com.example.common.event.BaseEvent;
import com.example.common.event.IdempotentEventProcessor;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Configuration
public class TestConfig {

    @Bean
    @Primary
    public IdempotentEventProcessor idempotentEventProcessor() {
        IdempotentEventProcessor mock = Mockito.mock(IdempotentEventProcessor.class);
        Mockito.doAnswer(invocation -> {
            Consumer<BaseEvent> handler = invocation.getArgument(1);
            handler.accept(invocation.getArgument(0));
            return null;
        }).when(mock).process(Mockito.any(BaseEvent.class), Mockito.any(Consumer.class));
        return mock;
    }

    @Bean
    @Primary
    public TimeLimiterRegistry timeLimiterRegistry() {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofMinutes(5))
                .build();
        TimeLimiterRegistry registry = TimeLimiterRegistry.of(config);
        
        // Register a no-op TimeLimiter that doesn't enforce timeouts
        TimeLimiter timeLimiter = TimeLimiter.of("test-time-limiter", config);
        registry.timeLimiter("inventory-service", config);
        return registry;
    }
}
