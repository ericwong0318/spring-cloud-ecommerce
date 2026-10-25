package com.example.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.example.common.event.CommonProcessedEventRepositoryImpl;
import com.example.common.event.JpaOutboxEventPublisher;
import com.example.common.event.OutboxPublisherAutoConfiguration;
import com.example.common.event.ProcessedEventEntityRepository;

@SpringBootApplication
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
}
