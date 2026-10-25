package com.example.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.example.common.event.CommonProcessedEventRepository;
import com.example.common.event.ReactiveIdempotentEventProcessor;

@Configuration
public class OrderReactiveConfig {

    @Bean
    public ReactiveIdempotentEventProcessor reactiveIdempotentEventProcessor(
            CommonProcessedEventRepository processedEventRepository,
            R2dbcTransactionManager transactionManager) {

        TransactionalOperator transactionalOperator = TransactionalOperator.create(transactionManager);
        return new ReactiveIdempotentEventProcessor(processedEventRepository, transactionalOperator);
    }
}
