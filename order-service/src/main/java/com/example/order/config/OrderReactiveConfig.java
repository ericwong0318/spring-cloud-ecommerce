package com.example.order.config;

import io.r2dbc.spi.ConnectionFactory;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;

import javax.sql.DataSource;
import com.example.common.event.CommonProcessedEventRepository;
import com.example.common.event.ReactiveIdempotentEventProcessor;

@Configuration
public class OrderReactiveConfig {

    @Bean
    public R2dbcTransactionManager transactionManager(ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
    }

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl("jdbc:postgresql://postgres-order:5432/order_db");
        dataSource.setUsername(System.getenv("DB_USERNAME"));
        dataSource.setPassword(System.getenv("DB_PASSWORD"));
        return dataSource;
    }

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(new JdbcTemplate(dataSource));
    }

    @Bean
    public ReactiveIdempotentEventProcessor reactiveIdempotentEventProcessor(
            CommonProcessedEventRepository processedEventRepository,
            R2dbcTransactionManager transactionManager) {

        TransactionalOperator transactionalOperator = TransactionalOperator.create(transactionManager);
        return new ReactiveIdempotentEventProcessor(processedEventRepository, transactionalOperator);
    }
}
