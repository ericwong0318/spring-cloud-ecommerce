package com.example.order;

import com.example.common.event.BaseEvent;
import com.example.common.event.ReactiveIdempotentEventProcessor;
import com.example.order.service.OrderService;
import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.testcontainers.containers.PostgreSQLContainer;
import reactor.core.publisher.Mono;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.function.Function;

/**
 * Base integration test configuration for order-service.
 * Uses Testcontainers PostgreSQL with manual Flyway migration execution
 * to avoid Flyway version detection issues with newer PostgreSQL versions.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {TestOrderServiceApplication.class},
    properties = {
        "spring.main.web-application-type=reactive",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration," +
        "org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration," +
        "org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration," +
        "org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration," +
        "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
        "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration," +
        "org.springframework.boot.autoconfigure.orm.jpa.JpaRepositoriesAutoConfiguration," +
        "org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration," +
        "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration," +
        "org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration," +
        "spring.main.allow-bean-definition-overriding=true",
        "spring.component.scan.exclude-filters=com.example.order.pact.*;com.example.order.*PactProviderTest.*;com.example.order.*PactConsumerTest.*;com.example.order.*PactTestConfig.*;com.example.order.service.ShipmentService;com.example.order.controller.ShipmentController;com.example.order.service.OrderService"
    }
)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import({MinimalTestConfig.class, BaseIntegrationTest.TestTransactionalOperatorConfig.class, BaseIntegrationTest.TestIdempotentEventProcessorConfig.class, TestSecurityConfig.class, BaseIntegrationTest.MockRabbitMQConfig.class, BaseIntegrationTest.DataSourceConfig.class})
public abstract class BaseIntegrationTest {

    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13.10-alpine")
            .withDatabaseName("order_db")
            .withUsername("postgres")
            .withPassword("postgres")
            .withStartupTimeout(Duration.ofMinutes(3))
            .withCommand("postgres -c fsync=off -c synchronous_commit=off -c full_page_writes=off");

    static {
        System.setProperty("testcontainers.ryuk.enabled", "true"); // Enable Ryuk for cleanup
        postgres.start();
        runFlywayMigrations();
    }

    private static void runFlywayMigrations() {
        try {
            // Run migration scripts directly using JDBC to bypass Flyway version detection issues
            String jdbcUrl = String.format("jdbc:postgresql://host.docker.internal:%d/%s",
                    postgres.getFirstMappedPort(), postgres.getDatabaseName());
            org.apache.commons.dbcp2.BasicDataSource dataSource = new org.apache.commons.dbcp2.BasicDataSource();
            dataSource.setUrl(String.format("jdbc:postgresql://host.docker.internal:%d/%s",
                    postgres.getFirstMappedPort(), postgres.getDatabaseName()));
            dataSource.setUsername(postgres.getUsername());
            dataSource.setPassword(postgres.getPassword());
            dataSource.setDriverClassName("org.postgresql.Driver");
            
            try (java.sql.Connection conn = dataSource.getConnection()) {
                // Run each migration script in order
                String[] migrationScripts = {
                    "db/migration/V1__create_order_table.sql",
                    "db/migration/V2__create_outbox_table.sql",
                    "db/migration/V3__create_processed_events_table.sql",
                    "db/migration/V4__create_order_item_table.sql",
                    "db/migration/V4__create_shedlock_table.sql",
                    "db/migration/V5__create_shipment_tables.sql"
                };
                
                for (String script : migrationScripts) {
                    System.out.println("Running migration: " + script);
                    try (java.io.InputStream is = BaseIntegrationTest.class.getClassLoader().getResourceAsStream(script)) {
                        if (is == null) {
                            throw new RuntimeException("Migration script not found: " + script);
                        }
                        String sql = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                        // Split by semicolon and execute each statement
                        String[] statements = sql.split(";");
                        for (String statement : statements) {
                            String trimmed = statement.trim();
                            if (!trimmed.isEmpty()) {
                                // Remove leading comments
                                while (trimmed.startsWith("--")) {
                                    int newlineIndex = trimmed.indexOf('\n');
                                    if (newlineIndex == -1) {
                                        trimmed = "";
                                        break;
                                    }
                                    trimmed = trimmed.substring(newlineIndex + 1).trim();
                                }
                                if (!trimmed.isEmpty()) {
                                    System.out.println("Executing: " + trimmed.substring(0, Math.min(200, trimmed.length())) + "...");
                                    try (java.sql.Statement stmt = conn.createStatement()) {
                                        stmt.execute(trimmed);
                                        System.out.println("  -> Success");
                                    } catch (Exception e) {
                                        System.err.println("  -> FAILED: " + e.getMessage());
                                        throw e;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            dataSource.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to run database migrations", e);
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () -> String.format("r2dbc:postgresql://%s:%d/%s",
                postgres.getHost(), postgres.getFirstMappedPort(), postgres.getDatabaseName()));
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
        
        // JDBC DataSource for Flyway migrations (manual)
        registry.add("spring.datasource.url", () -> String.format("jdbc:postgresql://%s:%d/%s",
                postgres.getHost(), postgres.getFirstMappedPort(), postgres.getDatabaseName()));
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        
        // Use mock RabbitMQ properties
        registry.add("spring.rabbitmq.host", () -> "localhost");
        registry.add("spring.rabbitmq.port", () -> 5672);
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");
        registry.add("spring.rabbitmq.publisher-confirm-type", () -> "correlated");
        registry.add("spring.rabbitmq.publisher-returns", () -> "true");
        registry.add("eureka.client.enabled", () -> "false");
        
        // RabbitMQ exchange/queue/routing-key configuration
        registry.add("rabbitmq.exchange.payment", () -> "payment.exchange");
        registry.add("rabbitmq.exchange.order", () -> "order.exchange");
        registry.add("rabbitmq.exchange.inventory", () -> "inventory.exchange");
        registry.add("rabbitmq.exchange.ecommerce", () -> "ecommerce.exchange");
        registry.add("rabbitmq.queue.order-events", () -> "order.events.queue");
        registry.add("rabbitmq.queue.payment-events", () -> "payment.events.queue");
        registry.add("rabbitmq.queue.inventory-events", () -> "inventory.events.queue");
        registry.add("rabbitmq.queue.reservation-expired", () -> "reservation.expired.queue");
        registry.add("rabbitmq.routing-key.order-created", () -> "order.created");
        registry.add("rabbitmq.routing-key.order-updated", () -> "order.updated");
        registry.add("rabbitmq.routing-key.order-cancelled", () -> "order.cancelled");
        registry.add("rabbitmq.routing-key.payment-authorized", () -> "payment.authorized");
        registry.add("rabbitmq.routing-key.payment-captured", () -> "payment.captured");
        registry.add("rabbitmq.routing-key.payment-refunded", () -> "payment.refunded");
        registry.add("rabbitmq.routing-key.payment-failed", () -> "payment.failed");
        registry.add("rabbitmq.routing-key.inventory-reserved", () -> "inventory.reserved");
        registry.add("rabbitmq.routing-key.reservation-expired", () -> "reservation.expired");
    }

    /**
     * Provides a mock RabbitMQ ConnectionFactory for tests.
     * This avoids the need for a real RabbitMQ container in basic integration tests.
     */
    @Configuration
    public static class MockRabbitMQConfig {
        @Bean
        @Primary
        public org.springframework.amqp.rabbit.connection.ConnectionFactory rabbitConnectionFactory() {
            return Mockito.mock(org.springframework.amqp.rabbit.connection.ConnectionFactory.class);
        }
    }

    /**
     * Provides a DataSource for Flyway migrations.
     */
    @Configuration
    public static class DataSourceConfig {
        @Bean
        @Primary
        public DataSource dataSource() {
            org.apache.commons.dbcp2.BasicDataSource dataSource = new org.apache.commons.dbcp2.BasicDataSource();
            dataSource.setUrl(String.format("jdbc:postgresql://%s:%d/%s",
                    postgres.getHost(), postgres.getFirstMappedPort(), postgres.getDatabaseName()));
            dataSource.setUsername(postgres.getUsername());
            dataSource.setPassword(postgres.getPassword());
            dataSource.setDriverClassName("org.postgresql.Driver");
            return dataSource;
        }
    }

    /**
     * Provides a TransactionalOperator for reactive transactions in tests.
     * Uses the auto-configured R2DBC ConnectionFactory.
     */
    @Configuration
    public static class TestTransactionalOperatorConfig {

        @Bean
        public org.springframework.r2dbc.connection.R2dbcTransactionManager r2dbcTransactionManager(io.r2dbc.spi.ConnectionFactory connectionFactory) {
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
                Function<com.example.common.event.BaseEvent, reactor.core.publisher.Mono<Void>> handler = invocation.getArgument(1);
                return handler.apply(invocation.getArgument(0));
            }).when(mock).process(Mockito.any(com.example.common.event.BaseEvent.class), Mockito.any(Function.class));
            return mock;
        }
    }
}
