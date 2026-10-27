package com.example.payment;

import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.example.common.event.CommonProcessedEventRepositoryImpl;
import com.example.common.event.JpaOutboxEventPublisher;
import com.example.common.event.OutboxPublisherAutoConfiguration;
import com.example.common.event.ProcessedEventEntityRepository;

@SpringBootApplication(exclude = FlywayAutoConfiguration.class)
@ComponentScan(basePackages = {"com.example.payment", "com.example.common"}, excludeFilters = {
    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        CommonProcessedEventRepositoryImpl.class,
        ProcessedEventEntityRepository.class,
        JpaOutboxEventPublisher.class,
        OutboxPublisherAutoConfiguration.class
    })
})
@EnableDiscoveryClient
@EnableR2dbcRepositories(basePackages = {"com.example.payment.repository", "com.example.payment.event"})
@EnableTransactionManagement
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner initSchema(R2dbcEntityTemplate template) {
        return args -> {
            template.getDatabaseClient().sql("CREATE TABLE IF NOT EXISTS payments (" +
                    "id BIGSERIAL PRIMARY KEY, " +
                    "order_id BIGINT NOT NULL, " +
                    "amount NUMERIC(10, 2) NOT NULL, " +
                    "currency VARCHAR(3) NOT NULL, " +
                    "status VARCHAR(20) NOT NULL, " +
                    "gateway_transaction_id VARCHAR(100), " +
                    "idempotency_key VARCHAR(100) NOT NULL UNIQUE, " +
                    "authorized_at TIMESTAMP, " +
                    "captured_at TIMESTAMP, " +
                    "refunded_at TIMESTAMP, " +
                    "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)").then().block();

            template.getDatabaseClient().sql("CREATE TABLE IF NOT EXISTS processed_events (" +
                    "event_id UUID PRIMARY KEY, " +
                    "processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)").then().block();

            template.getDatabaseClient().sql("CREATE TABLE IF NOT EXISTS outbox_event (" +
                    "id UUID PRIMARY KEY, " +
                    "aggregate_type VARCHAR(100) NOT NULL, " +
                    "aggregate_id VARCHAR(100) NOT NULL, " +
                    "event_type VARCHAR(100) NOT NULL, " +
                    "payload JSONB NOT NULL, " +
                    "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "published_at TIMESTAMP, " +
                    "retry_count INTEGER NOT NULL DEFAULT 0)").then().block();
        };
    }
}