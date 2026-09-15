package com.example.order;

import com.example.common.event.BaseEvent;
import com.example.order.event.ReactiveIdempotentEventProcessor;
import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import reactor.core.publisher.Mono;

import java.util.function.Function;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {MinimalTestConfig.class, BaseIntegrationTest.TestTransactionalOperatorConfig.class, BaseIntegrationTest.TestIdempotentEventProcessorConfig.class},
    properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration," +
        "org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration," +
        "org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration," +
        "org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration," +
        "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
        "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
        "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration," +
        "org.springframework.boot.autoconfigure.orm.jpa.JpaRepositoriesAutoConfiguration," +
        "spring.main.allow-bean-definition-overriding=true"
    }
)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseIntegrationTest {

    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("order_db")
            .withUsername("postgres")
            .withPassword("postgres");

    static final RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.13-management-alpine")
            .withExposedPorts(5672, 15672);

    static {
        postgres.start();
        rabbitmq.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () -> String.format("r2dbc:postgresql://%s:%d/%s",
                postgres.getHost(), postgres.getFirstMappedPort(), postgres.getDatabaseName()));
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitmq::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitmq::getAdminPassword);
        registry.add("spring.rabbitmq.publisher-confirm-type", () -> "correlated");
        registry.add("spring.rabbitmq.publisher-returns", () -> "true");
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    /**
     * Provides a TransactionalOperator for reactive transactions in tests.
     * Uses the auto-configured R2DBC ConnectionFactory.
     */
    @Configuration
    public static class TestTransactionalOperatorConfig {

        @Bean
        public org.springframework.r2dbc.connection.R2dbcTransactionManager r2dbcTransactionManager(ConnectionFactory connectionFactory) {
            return new org.springframework.r2dbc.connection.R2dbcTransactionManager(connectionFactory);
        }

        @Bean
        public TransactionalOperator transactionalOperator(org.springframework.r2dbc.connection.R2dbcTransactionManager transactionManager) {
            return TransactionalOperator.create(transactionManager);
        }
    }

    /**
     * Mock ReactiveIdempotentEventProcessor for integration tests to avoid JPA repository dependency.
     * Delegates to the actual handler to test the business logic.
     */
    @Configuration
    public static class TestIdempotentEventProcessorConfig {

        @Bean
        @Primary
        public ReactiveIdempotentEventProcessor reactiveIdempotentEventProcessor() {
            ReactiveIdempotentEventProcessor mock = Mockito.mock(ReactiveIdempotentEventProcessor.class);
            Mockito.doAnswer(invocation -> {
                Function<BaseEvent, Mono<Void>> handler = invocation.getArgument(1);
                return handler.apply(invocation.getArgument(0));
            }).when(mock).process(Mockito.any(BaseEvent.class), Mockito.any(Function.class));
            return mock;
        }
    }
}