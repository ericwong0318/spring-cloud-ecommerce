package com.example.order.config;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;

@Configuration
@EnableR2dbcRepositories(basePackages = "com.example.order.outbox")
public class OutboxConfig {

    @Bean
    public TransactionalOperator transactionalOperator(ConnectionFactory connectionFactory) {
        R2dbcTransactionManager transactionManager = new R2dbcTransactionManager(connectionFactory);
        return TransactionalOperator.create(transactionManager);
    }
}