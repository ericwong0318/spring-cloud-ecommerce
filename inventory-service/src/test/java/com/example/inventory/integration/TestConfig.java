package com.example.inventory.integration;

import com.example.common.event.BaseEvent;
import com.example.common.event.IdempotentEventProcessor;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.function.Consumer;

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
}
