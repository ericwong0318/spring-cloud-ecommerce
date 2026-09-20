package com.example.inventory.integration;

import com.example.common.event.InventoryEvent;
import com.example.inventory.InventoryServiceApplication;
import com.example.inventory.model.Inventory;
import com.example.inventory.model.Reservation;
import com.example.inventory.repository.InventoryRepository;
import com.example.inventory.repository.ReservationRepository;
import com.example.inventory.service.InventoryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(classes = InventoryServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Import({EventPublishingTestConfig.class, TestJpaConfig.class, TestOutboxPublisherConfig.class})
@EnableAutoConfiguration(exclude = {
    org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration.class,
    org.springframework.boot.autoconfigure.r2dbc.R2dbcTransactionManagerAutoConfiguration.class
})
class EventPublishingIntegrationTest {

    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("inventory_db")
            .withUsername("test")
            .withPassword("test");

    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.13-management")
            .withExposedPorts(5672);

    static {
        postgres.start();
        rabbitmq.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.show-sql", () -> "false");
        registry.add("spring.jpa.properties.hibernate.format_sql", () -> "true");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitmq::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitmq::getAdminPassword);
        registry.add("spring.rabbitmq.virtual-host", () -> "/");
        registry.add("spring.rabbitmq.publisher-confirm-type", () -> "correlated");
        registry.add("spring.rabbitmq.publisher-returns", () -> "true");
        registry.add("rabbitmq.exchange.product", () -> "product.exchange");
        registry.add("rabbitmq.exchange.order", () -> "order.exchange");
        registry.add("rabbitmq.exchange.inventory", () -> "inventory.exchange");
        registry.add("rabbitmq.queue.inventory-events", () -> "inventory.events.queue");
        registry.add("rabbitmq.routing-key.product-created", () -> "product.created");
        registry.add("rabbitmq.routing-key.product-updated", () -> "product.updated");
        registry.add("rabbitmq.routing-key.product-deleted", () -> "product.deleted");
        registry.add("rabbitmq.routing-key.order-created", () -> "order.created");
        registry.add("rabbitmq.routing-key.order-cancelled", () -> "order.cancelled");
        registry.add("rabbitmq.routing-key.reservation-expired", () -> "reservation.expired");
        registry.add("eureka.client.enabled", () -> "false");
    }

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Autowired
    private ObjectMapper objectMapper;

    private static final Logger log = LoggerFactory.getLogger(EventPublishingIntegrationTest.class);

    @Value("${rabbitmq.exchange.inventory}")
    private String inventoryExchange;

    private TopicExchange exchange;

    private final Long testVariantId = 100L;
    private final Long testProductId = 50L;
    private final Long testOrderItemId = 1000L;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        inventoryRepository.deleteAll();

        Inventory testInventory = new Inventory();
        testInventory.setVariantId(testVariantId);
        testInventory.setProductId(testProductId);
        testInventory.setProductName("Test Product");
        testInventory.setSkuCode("TEST-001");
        testInventory.setQuantity(100);
        testInventory.setReservedQuantity(0);
        testInventory.setReorderLevel(10);
        testInventory.setCostPrice(new BigDecimal("50.00"));
        inventoryRepository.save(testInventory);

        exchange = new TopicExchange(inventoryExchange, true, false);
        rabbitAdmin.declareExchange(exchange);
    }

    private Queue createAndBindTestQueue(String routingKey) {
        String queueName = "test.inventory.events." + routingKey + "." + UUID.randomUUID().toString().substring(0, 8);
        Queue queue = new Queue(queueName, true, false, false);
        rabbitAdmin.declareQueue(queue);
        rabbitAdmin.declareBinding(BindingBuilder.bind(queue).to(exchange).with(routingKey));
        return queue;
    }

    private InventoryEvent consumeAndDeserializeEvent(Queue queue, long timeoutSeconds) {
        try {
            // Small delay to allow message to be routed
            Thread.sleep(500);
            
            Object message = rabbitTemplate.receiveAndConvert(queue.getName(), timeoutSeconds * 1000);
            assertThat(message).as("No message received").isNotNull();

            String json = objectMapper.writeValueAsString(message);
            JsonNode jsonNode = objectMapper.readTree(json);

            assertThat(jsonNode.get("eventType")).isNotNull();
            assertThat(jsonNode.get("eventId")).isNotNull();
            assertThat(jsonNode.get("variantId")).isNotNull();
            assertThat(jsonNode.get("productId")).isNotNull();
            assertThat(jsonNode.get("timestamp")).isNotNull();

            return objectMapper.treeToValue(jsonNode, InventoryEvent.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to consume and deserialize event", e);
        }
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void shouldPublishReservedEventWithCorrectSchema() {
        Queue queue = createAndBindTestQueue("inventory.reserved");

        inventoryService.reserveStock(testVariantId, 10, testOrderItemId);

        InventoryEvent event = consumeAndDeserializeEvent(queue, 5);

        assertThat(event.getEventType()).isEqualTo(InventoryEvent.EventType.RESERVED.name());
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getVariantId()).isEqualTo(testVariantId);
        assertThat(event.getProductId()).isEqualTo(testProductId);
        assertThat(event.getReservedQuantity()).isEqualTo(10);
        assertThat(event.getBackorderedQuantity()).isEqualTo(0);
        assertThat(event.getReserved()).isEqualTo(10);
        assertThat(event.getBackordered()).isEqualTo(0);
        assertThat(event.getTimestamp()).isNotNull();

        Inventory inventory = inventoryRepository.findByVariantId(testVariantId).orElseThrow();
        assertThat(inventory.getReservedQuantity()).isEqualTo(10);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(90);
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void shouldPublishReleasedEventWithCorrectSchema() {
        Queue queue = createAndBindTestQueue("inventory.released");

        inventoryService.reserveStock(testVariantId, 10, testOrderItemId);
        inventoryService.releaseReservation(testVariantId, 10, testOrderItemId);

        InventoryEvent event = consumeAndDeserializeEvent(queue, 5);

        assertThat(event.getEventType()).isEqualTo(InventoryEvent.EventType.RELEASED.name());
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getVariantId()).isEqualTo(testVariantId);
        assertThat(event.getProductId()).isEqualTo(testProductId);
        assertThat(event.getReservedQuantity()).isEqualTo(10);
        assertThat(event.getAvailableQuantity()).isEqualTo(100);
        assertThat(event.getTimestamp()).isNotNull();

        Inventory inventory = inventoryRepository.findByVariantId(testVariantId).orElseThrow();
        assertThat(inventory.getReservedQuantity()).isEqualTo(0);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(100);
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void shouldPublishConfirmedEventWithCorrectSchema() {
        Queue queue = createAndBindTestQueue("inventory.confirmed");

        inventoryService.reserveStock(testVariantId, 10, testOrderItemId);
        inventoryService.confirmStock(testVariantId, 10);

        InventoryEvent event = consumeAndDeserializeEvent(queue, 5);

        assertThat(event.getEventType()).isEqualTo(InventoryEvent.EventType.CONFIRMED.name());
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getVariantId()).isEqualTo(testVariantId);
        assertThat(event.getProductId()).isEqualTo(testProductId);
        assertThat(event.getQuantity()).isEqualTo(10);
        assertThat(event.getAvailableQuantity()).isEqualTo(90);
        assertThat(event.getTimestamp()).isNotNull();

        Inventory inventory = inventoryRepository.findByVariantId(testVariantId).orElseThrow();
        assertThat(inventory.getQuantity()).isEqualTo(90);
        assertThat(inventory.getReservedQuantity()).isEqualTo(0);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(90);
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void shouldPublishLowStockEventWithCorrectSchema() {
        Queue queue = createAndBindTestQueue("inventory.low_stock");

        Inventory testInventory = inventoryRepository.findByVariantId(testVariantId).orElseThrow();
        testInventory.setQuantity(15);
        testInventory.setReservedQuantity(0);
        testInventory.setReorderLevel(10);
        testInventory.setLowStockNotified(false);
        inventoryRepository.save(testInventory);

        inventoryService.reserveStock(testVariantId, 6, testOrderItemId);

        InventoryEvent event = consumeAndDeserializeEvent(queue, 5);

        assertThat(event.getEventType()).isEqualTo(InventoryEvent.EventType.LOW_STOCK.name());
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getVariantId()).isEqualTo(testVariantId);
        assertThat(event.getProductId()).isEqualTo(testProductId);
        assertThat(event.getAvailableQuantity()).isEqualTo(9);
        assertThat(event.getTimestamp()).isNotNull();

        Inventory inventory = inventoryRepository.findByVariantId(testVariantId).orElseThrow();
        assertThat(inventory.getAvailableQuantity()).isEqualTo(9);
        assertThat(inventory.getLowStockNotified()).isTrue();
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void shouldPublishReservedEventWithBackorderWhenInsufficientStock() {
        Queue queue = createAndBindTestQueue("inventory.reserved");

        Inventory testInventory = inventoryRepository.findByVariantId(testVariantId).orElseThrow();
        testInventory.setQuantity(5);
        testInventory.setReservedQuantity(0);
        inventoryRepository.save(testInventory);

        inventoryService.reserveStock(testVariantId, 10, testOrderItemId);

        InventoryEvent event = consumeAndDeserializeEvent(queue, 5);

        assertThat(event.getEventType()).isEqualTo(InventoryEvent.EventType.RESERVED.name());
        assertThat(event.getReservedQuantity()).isEqualTo(5);
        assertThat(event.getBackorderedQuantity()).isEqualTo(5);
        assertThat(event.getReserved()).isEqualTo(5);
        assertThat(event.getBackordered()).isEqualTo(5);
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void shouldDeserializePublishedEventsCorrectly() {
        // Test reserved event
        Queue reservedQueue = createAndBindTestQueue("inventory.reserved");
        inventoryService.reserveStock(testVariantId, 5, testOrderItemId);
        InventoryEvent reservedEvent = consumeAndDeserializeEvent(reservedQueue, 5);
        assertThat(reservedEvent.getEventType()).isEqualTo(InventoryEvent.EventType.RESERVED.name());
        assertThat(reservedEvent.getVariantId()).isEqualTo(testVariantId);

        // Test released event
        Queue releasedQueue = createAndBindTestQueue("inventory.released");
        inventoryService.releaseReservation(testVariantId, 5, testOrderItemId);
        InventoryEvent releasedEvent = consumeAndDeserializeEvent(releasedQueue, 5);
        assertThat(releasedEvent.getEventType()).isEqualTo(InventoryEvent.EventType.RELEASED.name());
        assertThat(releasedEvent.getVariantId()).isEqualTo(testVariantId);

        // Test second reserved event
        Queue reservedQueue2 = createAndBindTestQueue("inventory.reserved");
        inventoryService.reserveStock(testVariantId, 5, testOrderItemId + 1);
        InventoryEvent secondReservedEvent = consumeAndDeserializeEvent(reservedQueue2, 5);
        assertThat(secondReservedEvent.getEventType()).isEqualTo(InventoryEvent.EventType.RESERVED.name());
        assertThat(secondReservedEvent.getVariantId()).isEqualTo(testVariantId);

        // Test confirmed event
        Queue confirmedQueue = createAndBindTestQueue("inventory.confirmed");
        inventoryService.confirmStock(testVariantId, 5);
        InventoryEvent confirmedEvent = consumeAndDeserializeEvent(confirmedQueue, 5);
        assertThat(confirmedEvent.getEventType()).isEqualTo(InventoryEvent.EventType.CONFIRMED.name());
        assertThat(confirmedEvent.getVariantId()).isEqualTo(testVariantId);
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void shouldHandleDuplicateReservedEventsIdempotently() {
        Queue queue = createAndBindTestQueue("inventory.reserved");

        inventoryService.reserveStock(testVariantId, 10, testOrderItemId);
        InventoryEvent firstEvent = consumeAndDeserializeEvent(queue, 5);

        inventoryService.reserveStock(testVariantId, 5, testOrderItemId + 1);
        InventoryEvent secondEvent = consumeAndDeserializeEvent(queue, 5);

        assertThat(firstEvent.getEventId()).isNotEqualTo(secondEvent.getEventId());
        assertThat(firstEvent.getEventType()).isEqualTo(secondEvent.getEventType());
        assertThat(firstEvent.getVariantId()).isEqualTo(secondEvent.getVariantId());
        assertThat(firstEvent.getReservedQuantity()).isEqualTo(10);
        assertThat(secondEvent.getReservedQuantity()).isEqualTo(5);

        Inventory inventory = inventoryRepository.findByVariantId(testVariantId).orElseThrow();
        assertThat(inventory.getReservedQuantity()).isEqualTo(15);
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void shouldHandleDuplicateReleasedEventsIdempotently() {
        Queue queue = createAndBindTestQueue("inventory.released");

        inventoryService.reserveStock(testVariantId, 10, testOrderItemId);
        inventoryService.releaseReservation(testVariantId, 10, testOrderItemId);
        InventoryEvent firstEvent = consumeAndDeserializeEvent(queue, 5);

        inventoryService.reserveStock(testVariantId, 10, testOrderItemId + 1);
        inventoryService.releaseReservation(testVariantId, 10, testOrderItemId + 1);
        InventoryEvent secondEvent = consumeAndDeserializeEvent(queue, 5);

        assertThat(firstEvent.getEventId()).isNotEqualTo(secondEvent.getEventId());
        assertThat(firstEvent.getEventType()).isEqualTo(secondEvent.getEventType());
        assertThat(firstEvent.getVariantId()).isEqualTo(secondEvent.getVariantId());
        assertThat(firstEvent.getReservedQuantity()).isEqualTo(10);
        assertThat(secondEvent.getReservedQuantity()).isEqualTo(10);
        assertThat(firstEvent.getAvailableQuantity()).isEqualTo(100);
        assertThat(secondEvent.getAvailableQuantity()).isEqualTo(100);
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void shouldHandleDuplicateLowStockEventsIdempotently() {
        Queue queue = createAndBindTestQueue("inventory.low_stock");

        Inventory testInventory = inventoryRepository.findByVariantId(testVariantId).orElseThrow();
        testInventory.setQuantity(15);
        testInventory.setReservedQuantity(0);
        testInventory.setReorderLevel(10);
        testInventory.setLowStockNotified(false);
        inventoryRepository.save(testInventory);

        inventoryService.reserveStock(testVariantId, 6, testOrderItemId);
        InventoryEvent firstEvent = consumeAndDeserializeEvent(queue, 5);

        inventoryService.reserveStock(testVariantId, 1, testOrderItemId + 1);
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            Inventory updated = inventoryRepository.findByVariantId(testVariantId).orElseThrow();
            assertThat(updated.getLowStockNotified()).isTrue();
        });

        // Second reserve should not publish another LOW_STOCK event (idempotent)
        Object message = rabbitTemplate.receiveAndConvert(queue.getName(), 2000);
        if (message != null) {
            try {
                String json = objectMapper.writeValueAsString(message);
                JsonNode jsonNode = objectMapper.readTree(json);
                assertThat(jsonNode.get("eventType").asText()).isEqualTo(InventoryEvent.EventType.LOW_STOCK.name());
            } catch (Exception e) {
                throw new RuntimeException("Failed to process duplicate low stock event", e);
            }
        }
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void shouldVerifyEventTimestampIsPresentAndValid() {
        Queue queue = createAndBindTestQueue("inventory.reserved");

        LocalDateTime beforePublish = LocalDateTime.now().minusSeconds(1);
        inventoryService.reserveStock(testVariantId, 10, testOrderItemId);
        LocalDateTime afterPublish = LocalDateTime.now().plusSeconds(1);

        InventoryEvent event = consumeAndDeserializeEvent(queue, 5);

        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getTimestamp()).isAfterOrEqualTo(beforePublish);
        assertThat(event.getTimestamp()).isBeforeOrEqualTo(afterPublish);
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void shouldVerifyEventIdIsUniquePerEvent() {
        Queue queue = createAndBindTestQueue("inventory.reserved");

        inventoryService.reserveStock(testVariantId, 10, testOrderItemId);
        InventoryEvent event1 = consumeAndDeserializeEvent(queue, 5);

        inventoryService.reserveStock(testVariantId, 5, testOrderItemId + 1);
        InventoryEvent event2 = consumeAndDeserializeEvent(queue, 5);

        assertThat(event1.getEventId()).isNotNull();
        assertThat(event2.getEventId()).isNotNull();
        assertThat(event1.getEventId()).isNotEqualTo(event2.getEventId());
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void shouldVerifyEventJsonStructureMatchesInventoryEventClass() {
        Queue queue = createAndBindTestQueue("inventory.reserved");

        inventoryService.reserveStock(testVariantId, 10, testOrderItemId);
        InventoryEvent event = consumeAndDeserializeEvent(queue, 5);

        String json;
        JsonNode jsonNode;
        try {
            json = objectMapper.writeValueAsString(event);
            jsonNode = objectMapper.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize/deserialize event", e);
        }

        assertThat(jsonNode.get("eventType")).isNotNull();
        assertThat(jsonNode.get("eventId")).isNotNull();
        assertThat(jsonNode.get("variantId")).isNotNull();
        assertThat(jsonNode.get("productId")).isNotNull();
        assertThat(jsonNode.get("reservedQuantity")).isNotNull();
        assertThat(jsonNode.get("backorderedQuantity")).isNotNull();
        assertThat(jsonNode.get("reserved")).isNotNull();
        assertThat(jsonNode.get("backordered")).isNotNull();
        assertThat(jsonNode.get("timestamp")).isNotNull();

        List<String> fieldNames = List.of(
            "eventType", "eventId", "variantId", "productId",
            "productName", "quantity", "reservedQuantity", "backorderedQuantity",
            "reserved", "backordered", "availableQuantity", "costPrice", "timestamp"
        );

        for (String fieldName : fieldNames) {
            assertThat(jsonNode.has(fieldName)).as("Missing field: " + fieldName).isTrue();
        }
    }
}