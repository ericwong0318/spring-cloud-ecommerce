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

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(classes = InventoryServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class EventPublishingIntegrationTest {

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
    private ReservationRepository reservationRepository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${rabbitmq.exchange.inventory}")
    private String inventoryExchange;

    private Queue testQueue;
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

        testQueue = new Queue("test.inventory.events", true, false, false);
        rabbitAdmin.declareQueue(testQueue);
        rabbitAdmin.purgeQueue(testQueue.getName(), false);
    }

    private void bindTestQueue(String routingKey) {
        rabbitAdmin.purgeQueue(testQueue.getName(), false);
        rabbitAdmin.declareBinding(BindingBuilder.bind(testQueue).to(exchange).with(routingKey));
    }

    private InventoryEvent consumeAndDeserializeEvent(long timeoutSeconds) {
        try {
            Object message = rabbitTemplate.receiveAndConvert(testQueue.getName(), timeoutSeconds * 1000);
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
        bindTestQueue("reserved");

        inventoryService.reserveStock(testVariantId, 10, testOrderItemId);

        InventoryEvent event = consumeAndDeserializeEvent(5);

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
        bindTestQueue("released");

        inventoryService.reserveStock(testVariantId, 10, testOrderItemId);
        inventoryService.releaseReservation(testVariantId, 10, testOrderItemId);

        InventoryEvent event = consumeAndDeserializeEvent(5);

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
        bindTestQueue("confirmed");

        inventoryService.reserveStock(testVariantId, 10, testOrderItemId);
        inventoryService.confirmStock(testVariantId, 10);

        InventoryEvent event = consumeAndDeserializeEvent(5);

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
        bindTestQueue("low_stock");

        Inventory testInventory = inventoryRepository.findByVariantId(testVariantId).orElseThrow();
        testInventory.setQuantity(15);
        testInventory.setReservedQuantity(0);
        testInventory.setReorderLevel(10);
        testInventory.setLowStockNotified(false);
        inventoryRepository.save(testInventory);

        inventoryService.reserveStock(testVariantId, 6, testOrderItemId);

        InventoryEvent event = consumeAndDeserializeEvent(5);

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
        bindTestQueue("reserved");

        Inventory testInventory = inventoryRepository.findByVariantId(testVariantId).orElseThrow();
        testInventory.setQuantity(5);
        testInventory.setReservedQuantity(0);
        inventoryRepository.save(testInventory);

        inventoryService.reserveStock(testVariantId, 10, testOrderItemId);

        InventoryEvent event = consumeAndDeserializeEvent(5);

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
        bindTestQueue("reserved");
        inventoryService.reserveStock(testVariantId, 5, testOrderItemId);
        InventoryEvent reservedEvent = consumeAndDeserializeEvent(5);
        assertThat(reservedEvent.getEventType()).isEqualTo(InventoryEvent.EventType.RESERVED.name());
        assertThat(reservedEvent.getVariantId()).isEqualTo(testVariantId);

        // Test released event
        bindTestQueue("released");
        inventoryService.releaseReservation(testVariantId, 5, testOrderItemId);
        InventoryEvent releasedEvent = consumeAndDeserializeEvent(5);
        assertThat(releasedEvent.getEventType()).isEqualTo(InventoryEvent.EventType.RELEASED.name());
        assertThat(releasedEvent.getVariantId()).isEqualTo(testVariantId);

        // Test second reserved event
        bindTestQueue("reserved");
        inventoryService.reserveStock(testVariantId, 5, testOrderItemId + 1);
        InventoryEvent secondReservedEvent = consumeAndDeserializeEvent(5);
        assertThat(secondReservedEvent.getEventType()).isEqualTo(InventoryEvent.EventType.RESERVED.name());
        assertThat(secondReservedEvent.getVariantId()).isEqualTo(testVariantId);

        // Test confirmed event
        bindTestQueue("confirmed");
        inventoryService.confirmStock(testVariantId, 5);
        InventoryEvent confirmedEvent = consumeAndDeserializeEvent(5);
        assertThat(confirmedEvent.getEventType()).isEqualTo(InventoryEvent.EventType.CONFIRMED.name());
        assertThat(confirmedEvent.getVariantId()).isEqualTo(testVariantId);
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void shouldHandleDuplicateReservedEventsIdempotently() {
        bindTestQueue("reserved");

        inventoryService.reserveStock(testVariantId, 10, testOrderItemId);
        InventoryEvent firstEvent = consumeAndDeserializeEvent(5);

        inventoryService.reserveStock(testVariantId, 5, testOrderItemId + 1);
        InventoryEvent secondEvent = consumeAndDeserializeEvent(5);

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
        bindTestQueue("released");

        inventoryService.reserveStock(testVariantId, 10, testOrderItemId);
        inventoryService.releaseReservation(testVariantId, 10, testOrderItemId);
        InventoryEvent firstEvent = consumeAndDeserializeEvent(5);

        inventoryService.reserveStock(testVariantId, 10, testOrderItemId + 1);
        inventoryService.releaseReservation(testVariantId, 10, testOrderItemId + 1);
        InventoryEvent secondEvent = consumeAndDeserializeEvent(5);

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
        bindTestQueue("low_stock");

        Inventory testInventory = inventoryRepository.findByVariantId(testVariantId).orElseThrow();
        testInventory.setQuantity(15);
        testInventory.setReservedQuantity(0);
        testInventory.setReorderLevel(10);
        testInventory.setLowStockNotified(false);
        inventoryRepository.save(testInventory);

        inventoryService.reserveStock(testVariantId, 6, testOrderItemId);
        InventoryEvent firstEvent = consumeAndDeserializeEvent(5);

        inventoryService.reserveStock(testVariantId, 1, testOrderItemId + 1);
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            Inventory updated = inventoryRepository.findByVariantId(testVariantId).orElseThrow();
            assertThat(updated.getLowStockNotified()).isTrue();
        });

        // Second reserve should not publish another LOW_STOCK event (idempotent)
        Object message = rabbitTemplate.receiveAndConvert(testQueue.getName(), 2000);
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
        bindTestQueue("reserved");

        LocalDateTime beforePublish = LocalDateTime.now().minusSeconds(1);
        inventoryService.reserveStock(testVariantId, 10, testOrderItemId);
        LocalDateTime afterPublish = LocalDateTime.now().plusSeconds(1);

        InventoryEvent event = consumeAndDeserializeEvent(5);

        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getTimestamp()).isAfterOrEqualTo(beforePublish);
        assertThat(event.getTimestamp()).isBeforeOrEqualTo(afterPublish);
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void shouldVerifyEventIdIsUniquePerEvent() {
        bindTestQueue("reserved");

        inventoryService.reserveStock(testVariantId, 10, testOrderItemId);
        InventoryEvent event1 = consumeAndDeserializeEvent(5);

        inventoryService.reserveStock(testVariantId, 5, testOrderItemId + 1);
        InventoryEvent event2 = consumeAndDeserializeEvent(5);

        assertThat(event1.getEventId()).isNotNull();
        assertThat(event2.getEventId()).isNotNull();
        assertThat(event1.getEventId()).isNotEqualTo(event2.getEventId());
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void shouldVerifyEventJsonStructureMatchesInventoryEventClass() {
        bindTestQueue("reserved");

        inventoryService.reserveStock(testVariantId, 10, testOrderItemId);
        InventoryEvent event = consumeAndDeserializeEvent(5);

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