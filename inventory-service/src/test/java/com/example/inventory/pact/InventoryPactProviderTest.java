package com.example.inventory.pact;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.junitsupport.State;
import com.example.inventory.model.Inventory;
import com.example.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(PactVerificationInvocationContextProvider.class)
@Provider("inventory-service")
@PactFolder("../order-service/target/pacts")
class InventoryPactProviderTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("inventory_db")
            .withUsername("test")
            .withPassword("test");

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.13-management")
            .withExposedPorts(5672);

    @Autowired
    private InventoryRepository inventoryRepository;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl());
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.r2dbc.url", () -> "r2dbc:postgresql://" + postgres.getHost() + ":" + postgres.getFirstMappedPort() + "/" + postgres.getDatabaseName());
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");
        registry.add("eureka.client.enabled", () -> "false");
    }

    @LocalServerPort
    private int port;

    @BeforeEach
    void before(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("valid reserve stock request")
    void validReserveStockRequest(Map<String, Object> params) {
        Inventory inventory = new Inventory();
        inventory.setId(1L);
        inventory.setVariantId(1L);
        inventory.setProductId(1L);
        inventory.setProductName("Laptop Pro 15");
        inventory.setQuantity(100);
        inventory.setReservedQuantity(0);
        inventory.setReorderLevel(10);
        inventory.setCostPrice(new BigDecimal("500.00"));
        inventory.setCreatedAt(LocalDateTime.parse("2024-01-15T10:30:00"));
        inventory.setUpdatedAt(LocalDateTime.parse("2024-01-15T10:30:00"));
        inventoryRepository.save(inventory);
    }

    @State("valid confirm stock request")
    void validConfirmStockRequest(Map<String, Object> params) {
        Inventory inventory = new Inventory();
        inventory.setId(1L);
        inventory.setVariantId(1L);
        inventory.setProductId(1L);
        inventory.setProductName("Laptop Pro 15");
        inventory.setQuantity(95);
        inventory.setReservedQuantity(5);
        inventory.setReorderLevel(10);
        inventory.setCostPrice(new BigDecimal("500.00"));
        inventory.setCreatedAt(LocalDateTime.parse("2024-01-15T10:30:00"));
        inventory.setUpdatedAt(LocalDateTime.parse("2024-01-15T10:30:00"));
        inventoryRepository.save(inventory);
    }

    @State("reserve stock request - variantId null")
    void reserveStockRequestVariantIdNull(Map<String, Object> params) {
    }

    @State("reserve stock request - quantity null")
    void reserveStockRequestQuantityNull(Map<String, Object> params) {
    }

    @State("reserve stock request - quantity less than 1")
    void reserveStockRequestQuantityLessThanOne(Map<String, Object> params) {
    }

    @State("reserve stock request - orderItemId null")
    void reserveStockRequestOrderItemIdNull(Map<String, Object> params) {
    }

    @State("confirm stock request - variantId null")
    void confirmStockRequestVariantIdNull(Map<String, Object> params) {
    }

    @State("confirm stock request - quantity null")
    void confirmStockRequestQuantityNull(Map<String, Object> params) {
    }

    @State("confirm stock request - quantity less than 1")
    void confirmStockRequestQuantityLessThanOne(Map<String, Object> params) {
    }
}