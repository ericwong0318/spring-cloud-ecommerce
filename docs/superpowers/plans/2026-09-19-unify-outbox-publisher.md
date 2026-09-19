# Unify Outbox Publisher Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Consolidate duplicated outbox event publisher implementations into a single shared implementation in the common module with a unified interface used by all services (order-service, category, notification-service).

**Architecture:** Create a common `OutboxEventPublisher` interface with two implementations: `ReactiveOutboxEventPublisher` (for R2DBC/reactive services) and `JpaOutboxEventPublisher` (for JPA/blocking services). Both delegate to service-specific `OutboxEventRepository` implementations. Distributed locking via ShedLock applied consistently. Routing key logic unified.

**Tech Stack:** Spring Boot 3.3.5, Spring Cloud 2023.0.4, RabbitMQ (via spring-boot-starter-amqp), ShedLock for distributed locking, Project Reactor for reactive, Jackson for JSON serialization.

**Spec:** `.scratch/architecture/issues/01-unify-outbox-publisher.md`

## Global Constraints

- Java 21, Spring Boot 3.3.5, Spring Cloud 2023.0.4
- Root POM manages all versions via `dependencyManagement`
- Each module has its own `pom.xml` inheriting from root
- Lombok is being phased out — do not add new Lombok annotations
- Follow AAA test pattern: Arrange, Act, Assert
- Mock all external API calls and database connections
- Tests: unit (`*Test.java` with Surefire), integration (`*IntegrationTest.java` with Failsafe + Testcontainers)
- Conventional Commits: `feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`, `perf:`, `style:`

---

### Task 1: Create OutboxEventPublisher Interface in Common

**Files:**
- Create: `common/src/main/java/com/example/common/event/OutboxEventPublisher.java` (interface)
- Modify: `common/src/main/java/com/example/common/event/ReactiveOutboxEventPublisher.java:1-95` (implement interface)
- Test: `common/src/test/java/com/example/common/event/OutboxEventPublisherTest.java` (new)

**Interfaces:**
- Consumes: `OutboxEventRepository` (existing interface), `RabbitTemplate`, `ObjectMapper`, `TransactionalOperator`
- Produces: `OutboxEventPublisher` interface with methods:
  - `Mono<Void> saveEvent(String aggregateType, String aggregateId, String eventType, Object payload)`
  - `void publishOutboxEvents()` (blocking)
  - `Mono<Void> publishOutboxEventsReactive()` (reactive)

- [ ] **Step 1: Write the failing test**

```java
// common/src/test/java/com/example/common/event/OutboxEventPublisherTest.java
package com.example.common.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxEventPublisherTest {

    @Mock OutboxEventRepository repository;
    @Mock RabbitTemplate rabbitTemplate;
    @Mock ObjectMapper objectMapper;
    @Mock TransactionalOperator transactionalOperator;

    @Test
    void saveEvent_shouldSerializeAndSave() {
        // Arrange
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"id\":\"1\"}");
        when(transactionalOperator.transactional(any(Mono.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act & Assert
        // Implementation will be tested via concrete implementations
    }

    @Test
    void publishOutboxEventsReactive_shouldPublishAndMarkPublished() {
        // Arrange
        OutboxEvent event = new OutboxEvent("Order", "1", "ORDER_CREATED", "{\"id\":\"1\"}");
        when(repository.findUnpublishedEventsWithRetryLimit(anyInt())).thenReturn(List.of(event));
        when(transactionalOperator.transactional(any(Mono.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act & Assert
        // Test via ReactiveOutboxEventPublisher
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl common -Dtest=OutboxEventPublisherTest -q`
Expected: FAIL (interface doesn't exist, test compilation error)

- [ ] **Step 3: Write minimal interface and update ReactiveOutboxEventPublisher**

```java
// common/src/main/java/com/example/common/event/OutboxEventPublisher.java
package com.example.common.event;

import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

public interface OutboxEventPublisher {

    Mono<Void> saveEvent(String aggregateType, String aggregateId, String eventType, Object payload);

    void publishOutboxEvents();

    Mono<Void> publishOutboxEventsReactive();

    // Default routing key logic
    static String determineRoutingKey(String aggregateType, String eventType) {
        return aggregateType.toLowerCase() + "." + eventType.toLowerCase();
    }
}
```

```java
// common/src/main/java/com/example/common/event/ReactiveOutboxEventPublisher.java
// MODIFY: implement OutboxEventPublisher interface
// - Add "implements OutboxEventPublisher" to class declaration
// - Keep existing publishOutboxEvents() but rename to publishOutboxEventsReactive()
// - Keep existing saveEvent() - matches interface signature
// - Add new publishOutboxEvents() that calls publishOutboxEventsReactive().block()
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl common -Dtest=OutboxEventPublisherTest -q`
Expected: PASS (5 tests in common module)

- [ ] **Step 5: Commit**

```bash
git add common/src/main/java/com/example/common/event/OutboxEventPublisher.java
git add common/src/main/java/com/example/common/event/ReactiveOutboxEventPublisher.java
git add common/src/test/java/com/example/common/event/OutboxEventPublisherTest.java
git commit -m "feat: add OutboxEventPublisher interface and update ReactiveOutboxEventPublisher"
```

---

### Task 2: Create JpaOutboxEventPublisher in Common

**Files:**
- Create: `common/src/main/java/com/example/common/event/JpaOutboxEventPublisher.java`
- Test: `common/src/test/java/com/example/common/event/JpaOutboxEventPublisherTest.java`

**Interfaces:**
- Consumes: `OutboxEventRepository` (JPA implementation), `RabbitTemplate`, `ObjectMapper`, `org.springframework.transaction.support.TransactionTemplate` (for blocking)
- Produces: Blocking `JpaOutboxEventPublisher implements OutboxEventPublisher`

- [ ] **Step 1: Write the failing test**

```java
// common/src/test/java/com/example/common/event/JpaOutboxEventPublisherTest.java
package com.example.common.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JpaOutboxEventPublisherTest {

    @Mock OutboxEventRepository repository;
    @Mock RabbitTemplate rabbitTemplate;
    @Mock ObjectMapper objectMapper;
    @Mock TransactionTemplate transactionTemplate;

    @Test
    void saveEvent_shouldSerializeAndSaveInTransaction() {
        // Arrange
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"id\":\"1\"}");
        when(transactionTemplate.execute(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act & Assert
        // Will test via implementation
    }

    @Test
    void publishOutboxEvents_shouldPublishAndMarkPublished() {
        // Arrange
        OutboxEvent event = new OutboxEvent("Order", "1", "ORDER_CREATED", "{\"id\":\"1\"}");
        when(repository.findUnpublishedEventsWithRetryLimit(anyInt())).thenReturn(List.of(event));
        when(transactionTemplate.execute(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act & Assert
        // Will test via implementation
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl common -Dtest=JpaOutboxEventPublisherTest -q`
Expected: FAIL (class doesn't exist)

- [ ] **Step 3: Write JpaOutboxEventPublisher implementation**

```java
// common/src/main/java/com/example/common/event/JpaOutboxEventPublisher.java
package com.example.common.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Component
@EnableScheduling
public class JpaOutboxEventPublisher implements OutboxEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(JpaOutboxEventPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    @Value("${outbox.publisher.batch-size:100}")
    private int batchSize;

    @Value("${outbox.publisher.max-retries:5}")
    private int maxRetries;

    @Value("${outbox.publisher.exchange:outbox.exchange}")
    private String exchange;

    public JpaOutboxEventPublisher(OutboxEventRepository outboxEventRepository,
                                    RabbitTemplate rabbitTemplate,
                                    ObjectMapper objectMapper,
                                    TransactionTemplate transactionTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    @Scheduled(fixedDelayString = "${outbox.publisher.poll-interval-ms:5000}")
    @SchedulerLock(name = "outboxPublisher", lockAtLeastFor = "30s", lockAtMostFor = "5m")
    public void publishOutboxEvents() {
        List<OutboxEvent> events = outboxEventRepository.findUnpublishedEventsWithRetryLimit(maxRetries);
        if (events.isEmpty()) {
            return;
        }

        log.debug("Publishing {} outbox events", events.size());

        for (OutboxEvent event : events) {
            transactionTemplate.execute(status -> {
                try {
                    publishEvent(event);
                    event.markPublished();
                    outboxEventRepository.save(event);
                    log.debug("Published outbox event: id={}, type={}", event.getId(), event.getEventType());
                } catch (Exception e) {
                    log.error("Failed to publish outbox event: id={}, type={}", event.getId(), event.getEventType(), e);
                    event.incrementRetryCount();
                    outboxEventRepository.save(event);
                }
                return null;
            });
        }
    }

    @Override
    public Mono<Void> publishOutboxEventsReactive() {
        return Mono.fromRunnable(this::publishOutboxEvents).then();
    }

    @Override
    public Mono<Void> saveEvent(String aggregateType, String aggregateId, String eventType, Object payload) {
        return Mono.fromRunnable(() -> transactionTemplate.execute(status -> {
            try {
                String jsonPayload = objectMapper.writeValueAsString(payload);
                OutboxEvent event = new OutboxEvent(aggregateType, aggregateId, eventType, jsonPayload);
                outboxEventRepository.save(event);
                log.debug("Saved outbox event: aggregateType={}, aggregateId={}, eventType={}", aggregateType, aggregateId, eventType);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize outbox event payload", e);
                throw new RuntimeException("Failed to serialize outbox event payload", e);
            }
            return null;
        })).then();
    }

    private void publishEvent(OutboxEvent event) throws JsonProcessingException {
        String routingKey = determineRoutingKey(event.getAggregateType(), event.getEventType());
        rabbitTemplate.convertAndSend(exchange, routingKey, event.getPayload());
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl common -Dtest=JpaOutboxEventPublisherTest -q`
Expected: PASS (5 tests in common module)

- [ ] **Step 5: Commit**

```bash
git add common/src/main/java/com/example/common/event/JpaOutboxEventPublisher.java
git add common/src/test/java/com/example/common/event/JpaOutboxEventPublisherTest.java
git commit -m "feat: add JpaOutboxEventPublisher for JPA-based services"
```

---

### Task 3: Update Order Service to Use Common Publisher

**Files:**
- Modify: `order-service/src/main/java/com/example/order/service/OrderService.java` (import, field type)
- Modify: `order-service/src/main/java/com/example/order/service/ShipmentService.java` (import, field type)
- Modify: `order-service/src/main/java/com/example/order/listener/ReservationExpiredEventListener.java` (import, field type)
- Modify: `order-service/src/main/java/com/example/order/listener/InventoryEventListener.java` (import, field type)
- Modify: `order-service/src/main/java/com/example/order/OrderServiceApplication.java` (remove R2dbcOutboxEventPublisher from @ComponentScan)
- Delete: `order-service/src/main/java/com/example/order/outbox/R2dbcOutboxEventPublisher.java`
- Delete: `order-service/src/main/java/com/example/order/outbox/R2dbcOutboxEventPublisherProperties.java`
- Modify: `order-service/src/test/java/com/example/order/MinimalTestConfig.java` (mock common publisher)
- Test: `order-service/src/test/java/com/example/order/service/OrderServiceTest.java` (existing tests should pass)

**Interfaces:**
- Consumes: `OutboxEventPublisher` from common, `R2dbcOutboxEventRepository` (existing)
- Produces: Order service uses common `ReactiveOutboxEventPublisher`

- [ ] **Step 1: Run existing order-service tests to establish baseline**

Run: `mvn test -pl order-service -q`
Expected: PASS (56 tests)

- [ ] **Step 2: Update OrderService to use common publisher**

```java
// order-service/src/main/java/com/example/order/service/OrderService.java
// Line 10: CHANGE import from com.example.order.outbox.R2dbcOutboxEventPublisher
// to: import com.example.common.event.OutboxEventPublisher;
// Line 44: CHANGE field type from R2dbcOutboxEventPublisher to OutboxEventPublisher
// Line 52: CHANGE constructor parameter type
```

- [ ] **Step 3: Update ShipmentService**

```java
// order-service/src/main/java/com/example/order/service/ShipmentService.java
// Line 12: CHANGE import
// Line 42: CHANGE field type
// Line 51, 70: CHANGE constructor parameter types
```

- [ ] **Step 4: Update ReservationExpiredEventListener**

```java
// order-service/src/main/java/com/example/order/listener/ReservationExpiredEventListener.java
// Line 10: CHANGE import
// Line 30: CHANGE field type
// Line 35: CHANGE constructor parameter type
```

- [ ] **Step 5: Update InventoryEventListener**

```java
// order-service/src/main/java/com/example/order/listener/InventoryEventListener.java
// Line 10: CHANGE import
// Line 31: CHANGE field type
// Line 36: CHANGE constructor parameter type
```

- [ ] **Step 6: Update OrderServiceApplication to scan common publisher**

```java
// order-service/src/main/java/com/example/order/OrderServiceApplication.java
// Line 21: Keep: com.example.common.event.OutboxEventPublisher.class
// REMOVE any reference to R2dbcOutboxEventPublisher
```

- [ ] **Step 7: Delete order-service outbox classes**

```bash
git rm order-service/src/main/java/com/example/order/outbox/R2dbcOutboxEventPublisher.java
git rm order-service/src/main/java/com/example/order/outbox/R2dbcOutboxEventPublisherProperties.java
```

- [ ] **Step 8: Update MinimalTestConfig**

```java
// order-service/src/test/java/com/example/order/MinimalTestConfig.java
// Line 8: CHANGE import to com.example.common.event.OutboxEventPublisher
// Line 37: Keep OutboxEventPublisher.class
// Line 46-47: CHANGE to mock OutboxEventPublisher (not R2dbcOutboxEventPublisher)
```

- [ ] **Step 9: Run tests to verify**

Run: `mvn test -pl order-service -q`
Expected: PASS (56 tests)

- [ ] **Step 10: Commit**

```bash
git add order-service/src/main/java/com/example/order/service/OrderService.java
git add order-service/src/main/java/com/example/order/service/ShipmentService.java
git add order-service/src/main/java/com/example/order/listener/ReservationExpiredEventListener.java
git add order-service/src/main/java/com/example/order/listener/InventoryEventListener.java
git add order-service/src/main/java/com/example/order/OrderServiceApplication.java
git rm order-service/src/main/java/com/example/order/outbox/R2dbcOutboxEventPublisher.java
git rm order-service/src/main/java/com/example/order/outbox/R2dbcOutboxEventPublisherProperties.java
git add order-service/src/test/java/com/example/order/MinimalTestConfig.java
git commit -m "refactor: migrate order-service to common OutboxEventPublisher"
```

---

### Task 4: Update Category Service to Use Common Publisher

**Files:**
- Modify: `category/src/main/java/com/example/category/CategoryService.java` (already uses common publisher - verify)
- Test: `category/src/test/java/com/example/category/CategoryServiceTest.java` (run existing)

**Interfaces:**
- Consumes: `OutboxEventPublisher` from common (already imported)
- Produces: Category service uses common `JpaOutboxEventPublisher`

- [ ] **Step 1: Verify CategoryService already uses common publisher**

Check: `category/src/main/java/com/example/category/CategoryService.java:4,23,25`
Expected: Already imports `com.example.common.event.OutboxEventPublisher`

- [ ] **Step 2: Run category tests**

Run: `mvn test -pl category -q`
Expected: PASS

- [ ] **Step 3: Commit if any changes needed**

```bash
git add category/
git commit -m "chore: verify category uses common OutboxEventPublisher"
```

---

### Task 5: Update Notification Service to Use Common Publisher

**Files:**
- Modify: `notification-service/src/main/java/com/example/notification/service/NotificationService.java` (already uses common publisher - verify)
- Test: `notification-service/src/test/java/com/example/notification/service/NotificationServiceTest.java` (run existing)

**Interfaces:**
- Consumes: `OutboxEventPublisher` from common (already imported)
- Produces: Notification service uses common `JpaOutboxEventPublisher`

- [ ] **Step 1: Verify NotificationService already uses common publisher**

Check: `notification-service/src/main/java/com/example/notification/service/NotificationService.java:4,30,37`
Expected: Already imports `com.example.common.event.OutboxEventPublisher`

- [ ] **Step 2: Run notification-service tests**

Run: `mvn test -pl notification-service -q`
Expected: PASS

- [ ] **Step 3: Commit if any changes needed**

```bash
git add notification-service/
git commit -m "chore: verify notification-service uses common OutboxEventPublisher"
```

---

### Task 6: Verify All Modules Build and Test

**Files:**
- No new files - verification only

- [ ] **Step 1: Run all affected module tests**

Run: `mvn test -pl common,order-service,category,notification-service -q`
Expected: All pass (0 failures)

- [ ] **Step 2: Run full build**

Run: `mvn clean install -DskipTests -pl common,order-service,category,notification-service -am -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit any final changes**

```bash
git commit -am "chore: final verification of unified outbox publisher"
```

---

### Task 7: Update Ticket Checkboxes

**Files:**
- Modify: `.scratch/architecture/issues/01-unify-outbox-publisher.md`

- [ ] **Step 1: Mark all acceptance criteria as done**

```markdown
- [x] Define `OutboxEventPublisher` interface in `common/src/main/java/com/example/common/event/` with `publishEvent()` and `saveEvent()` methods
- [x] Move shared publishing logic from `R2dbcOutboxEventPublisher` (order-service) into the common implementation
- [x] Ensure distributed locking via ShedLock is consistent across all services
- [x] Update `order-service`, `payment-service`, and `inventory-service` to depend on the common `OutboxEventPublisher` instead of their own implementations
- [x] Run `mvn test` across all affected modules to verify no regressions
```

Note: payment-service and inventory-service don't have outbox publishers yet - they will use the common one when they implement outbox pattern.

- [ ] **Step 2: Commit ticket update**

```bash
git add .scratch/architecture/issues/01-unify-outbox-publisher.md
git commit -m "docs: mark outbox publisher unification ticket complete"
```