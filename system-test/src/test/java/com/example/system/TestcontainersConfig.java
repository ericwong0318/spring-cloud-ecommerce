package com.example.system;

import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class TestcontainersConfig {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("ecommerce_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static final MongoDBContainer mongodb = new MongoDBContainer("mongo:7.0")
            .withReuse(true);

    @Container
    static final RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.13-management-alpine")
            .withExposedPorts(5672, 15672);

    static {
        postgres.start();
        mongodb.start();
        rabbitmq.start();
    }
}