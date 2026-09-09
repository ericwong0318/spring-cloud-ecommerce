package com.example.inventory.integration;

import com.example.common.event.IdempotentEventProcessor;
import com.example.common.event.OrderEvent;
import com.example.common.event.ProductEvent;
import com.example.inventory.InventoryServiceApplication;
import com.example.inventory.model.Inventory;
import com.example.inventory.repository.InventoryRepository;
import com.example.inventory.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(classes = InventoryServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class IdempotentEventListenerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("inventory_db")
            .withUsername("test")
            .withPassword("test");

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.13-management")
            .withExposedPorts(5672);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("eureka.client.enabled", () -> "false");
    }

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Value("${rabbitmq.queue.inventory-events}")
    private String inventoryQueue;

    @Value("${rabbitmq.exchange.order}")
    private String orderExchange;

    @Value("${rabbitmq.routing-key.order-created}")
    private String orderCreatedRoutingKey;

    @Value("${rabbitmq.exchange.product}")
    private String productExchange;

    @Value("${rabbitmq.routing-key.product-created}")
    private String productCreatedRoutingKey;

    private final Long testVariantId = 100L;
    private final Long testProductId = 50L;
    private final Long testOrderId = 1000L;

    @BeforeEach
    void setUp() {
        inventoryRepository.deleteAll();
        // Ensure queue is declared in RabbitMQ
        rabbitAdmin.declareQueue(new Queue(inventoryQueue, true));
        // Purge any leftover messages from previous tests
        rabbitAdmin.purgeQueue(inventoryQueue, false);
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void shouldProcessProductEventOnlyOnceWhenDuplicateEventId() {
        // Given: A product created event with a specific eventId
        UUID eventId = UUID.randomUUID();
        ProductEvent event = new ProductEvent(
                ProductEvent.EventType.VARIANT_CREATED.name(),
                eventId,
                testProductId,
                "Test Product",  // productName
                new BigDecimal("99.99"),
                1L,              // categoryId
                testVariantId,
                "TEST-001",
                java.time.LocalDateTime.now()
        );

        // When: Send the same event twice using the exchange and routing key
        rabbitTemplate.convertAndSend(productExchange, productCreatedRoutingKey, event);
        rabbitTemplate.convertAndSend(productExchange, productCreatedRoutingKey, event);

        // Then: Wait for processing and verify inventory was created only once
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Inventory> inventories = inventoryRepository.findAll();
            assertThat(inventories).hasSize(1);
            Inventory inventory = inventories.get(0);
            assertThat(inventory.getVariantId()).isEqualTo(testVariantId);
            assertThat(inventory.getProductName()).isEqualTo("Test Product");
            assertThat(inventory.getSkuCode()).isEqualTo("TEST-001");
        });
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void shouldProcessOrderCreatedEventOnlyOnceWhenDuplicateEventId() {
        // Given: An order created event with a specific eventId
        UUID eventId = UUID.randomUUID();
        OrderEvent.OrderItem item = OrderEvent.OrderItem.builder()
                .orderItemId(1L)
                .productId(testProductId)
                .variantId(testVariantId)
                .productName("Test Product")
                .skuCode("TEST-001")
                .quantity(5)
                .quantityShipped(0)
                .price(new BigDecimal("99.99"))
                .status(OrderEvent.OrderItemStatus.PENDING)
                .build();

        // First create inventory so reservation can work
        Inventory inventory = new Inventory();
        inventory.setVariantId(testVariantId);
        inventory.setProductId(testProductId);
        inventory.setProductName("Test Product");
        inventory.setSkuCode("TEST-001");
        inventory.setQuantity(100);
        inventory.setReservedQuantity(0);
        inventory.setReorderLevel(10);
        inventory.setCostPrice(new BigDecimal("50.00"));
        inventoryRepository.save(inventory);

        OrderEvent event = OrderEvent.created(testOrderId, "customer-1", "test@example.com",
                new BigDecimal("499.95"), List.of(item));
        event.setEventId(eventId);

        System.out.println("Sending event to exchange: " + orderExchange + " with routing key: " + orderCreatedRoutingKey);
        System.out.println("Event: " + event);

        // When: Send the same event twice using the exchange and routing key
        rabbitTemplate.convertAndSend(orderExchange, orderCreatedRoutingKey, event);
        rabbitTemplate.convertAndSend(orderExchange, orderCreatedRoutingKey, event);

        // Then: Wait for processing and verify reservation happened only once
        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            Inventory updatedInventory = inventoryRepository.findByVariantId(testVariantId).orElseThrow();
            System.out.println("Inventory state: reserved=" + updatedInventory.getReservedQuantity() + ", available=" + updatedInventory.getAvailableQuantity());
            // Should only reserve 5 once, not 10
            assertThat(updatedInventory.getReservedQuantity()).isEqualTo(5);
            assertThat(updatedInventory.getAvailableQuantity()).isEqualTo(95);
        });
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void shouldProcessEventWithoutEventIdEveryTime() {
        // Given: A product event WITHOUT eventId (null)
        ProductEvent event = new ProductEvent(
                ProductEvent.EventType.VARIANT_CREATED.name(),
                null,
                testProductId,
                "Test Product",
                new BigDecimal("149.99"),
                1L,
                testVariantId + 1,
                "TEST-002",
                java.time.LocalDateTime.now()
        );

        // When: Send the same event twice (without eventId)
        rabbitTemplate.convertAndSend(productExchange, productCreatedRoutingKey, event);
        rabbitTemplate.convertAndSend(productExchange, productCreatedRoutingKey, event);

        // Then: Both should be processed (no deduplication without eventId)
        // Since it's a variant creation, second one will just update the same record
        // We verify the final state is correct
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Inventory inventory = inventoryRepository.findByVariantId(testVariantId + 1).orElseThrow();
            assertThat(inventory.getProductName()).isEqualTo("Test Product");
            assertThat(inventory.getSkuCode()).isEqualTo("TEST-002");
        });
    }
}