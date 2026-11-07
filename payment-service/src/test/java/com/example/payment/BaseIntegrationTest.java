package com.example.payment;

import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import org.springframework.core.env.Environment;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {PaymentServiceTestApplication.class, com.example.payment.config.TestSecurityConfig.class, BaseIntegrationTest.TestDataSourceConfig.class, BaseIntegrationTest.TestFlywayRunner.class})
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("payment_service")
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
        // OrbStack does not route host.docker.internal from the macOS host JVM.
        // Use localhost so the Spring app can reach Testcontainers' containers
        // via Docker's published ports on 127.0.0.1.
        String dbHost = "localhost";
        String rabbitHost = "localhost";

        // JDBC (Flyway) - point to the Testcontainers instance
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl().replace("host.docker.internal", dbHost));
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        // R2DBC (Application) - same container, dynamic port
        registry.add("spring.r2dbc.url", () -> String.format("r2dbc:postgresql://%s:%d/%s",
                dbHost, postgres.getFirstMappedPort(), postgres.getDatabaseName()));
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);

        // RabbitMQ
        registry.add("spring.rabbitmq.host", () -> rabbitHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitmq::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitmq::getAdminPassword);

        // Flyway & Eureka - Flyway auto-config disabled in application-test.yml, we run manually
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("eureka.client.enabled", () -> "false");
    }

    /**
     * Provides a JDBC DataSource for Flyway migrations.
     * The payment service uses R2DBC, so no DataSource bean exists by default.
     */
    @Configuration
    public static class TestDataSourceConfig {

        @Bean
        public DataSource dataSource() {
            HikariDataSource ds = new HikariDataSource();
            ds.setJdbcUrl(postgres.getJdbcUrl().replace("host.docker.internal", "localhost"));
            ds.setUsername(postgres.getUsername());
            ds.setPassword(postgres.getPassword());
            ds.setDriverClassName("org.postgresql.Driver");
            return ds;
        }
    }

    /**
     * Runs Flyway migrations after the application context is refreshed.
     * This ensures both JDBC (Flyway) and R2DBC (Application) use the same Testcontainers database.
     */
    @Configuration
    public static class TestFlywayRunner implements ApplicationListener<ContextRefreshedEvent> {

        private final DataSource dataSource;
        private final Environment environment;

        public TestFlywayRunner(DataSource dataSource, Environment environment) {
            this.dataSource = dataSource;
            this.environment = environment;
        }

        @Override
        public void onApplicationEvent(ContextRefreshedEvent event) {
            if ("false".equalsIgnoreCase(environment.getProperty("spring.flyway.enabled", "true"))) {
                return;
            }
            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .load();
            flyway.migrate();
        }
    }
}
