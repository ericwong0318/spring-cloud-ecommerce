package com.example.order;

import com.zaxxer.hikari.HikariDataSource;
import com.example.common.event.BaseEvent;
import com.example.common.event.IdempotentEventProcessor;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.function.Consumer;
import javax.sql.DataSource;

@Testcontainers
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {OrderServiceApplication.class, BaseIntegrationTest.TestDataSourceConfig.class, BaseIntegrationTest.TestTransactionalOperatorConfig.class, BaseIntegrationTest.TestIdempotentEventProcessorConfig.class},
    properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration," +
        "org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration," +
        "org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration," +
        "org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration," +
        "spring.main.allow-bean-definition-overriding=true"
    }
)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("order_db")
            .withUsername("postgres")
            .withPassword("postgres");

    @Container
    static final RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.13-management-alpine")
            .withExposedPorts(5672, 15672);

    static {
        postgres.start();
        rabbitmq.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.r2dbc.url", () -> String.format("r2dbc:postgresql://%s:%d/%s",
                postgres.getHost(), postgres.getFirstMappedPort(), postgres.getDatabaseName()));
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
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
     * Provides a JDBC DataSource for JPA repositories (common module's ProcessedEventRepository).
     * The order service uses R2DBC for its own repositories, but the common module uses JPA.
     */
    @Configuration
    public static class TestDataSourceConfig {

        @Bean
        public DataSource dataSource() {
            HikariDataSource ds = new HikariDataSource();
            ds.setJdbcUrl(postgres.getJdbcUrl());
            ds.setUsername(postgres.getUsername());
            ds.setPassword(postgres.getPassword());
            ds.setDriverClassName("org.postgresql.Driver");
            return ds;
        }
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
     * Mock IdempotentEventProcessor for integration tests to avoid JPA repository dependency.
     * Delegates to the actual handler to test the business logic.
     */
    @Configuration
    public static class TestIdempotentEventProcessorConfig {

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
}