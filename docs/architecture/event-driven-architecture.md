# Event-Driven Architecture: Consolidated Implementation

This document describes the consolidated event-driven architecture implemented across the Spring Cloud Microservices Platform. It covers the unified event infrastructure, saga orchestration patterns, and service decomposition completed through seven related improvements.

---

## Table of Contents

1. [Unified Outbox Pattern](#unified-outbox-pattern)
2. [Idempotent Event Processing](#idempotent-event-processing)
3. [Saga Orchestration](#saga-orchestration)
4. [Service Decomposition](#service-decomposition)
5. [Listener Abstraction](#listener-abstraction)
6. [Cross-Cutting Concerns](#cross-cutting-concerns)
7. [Testing and Verification](#testing-and-verification)

---

## Unified Outbox Pattern

### Problem
Multiple services implemented their own outbox event publishers with duplicated logic for payload serialization, routing key determination, and RabbitMQ publishing. The common module provided interfaces but no concrete implementations, leading to inconsistency and maintenance burden.

### Solution
A single `OutboxEventPublisher` interface with concrete implementations in the `common` module, supporting both R2DBC (reactive services) and JPA (traditional services) backends.

### Components

| Component | Location | Purpose |
|-----------|----------|---------|
| `OutboxEventPublisher` | `common/src/main/java/com/example/common/event/` | Unified interface with `saveEvent()` and `publishEvent()` |
| `ReactiveOutboxEventPublisher` | `common/src/main/java/com/example/common/event/` | R2DBC implementation for order, inventory, payment services |
| `JpaOutboxEventRepository` | `common/src/main/java/com/example/common/event/` | JPA implementation for product, category, notification services |
| `OutboxPublisherAutoConfiguration` | `common/src/main/java/com/example/common/event/` | Spring Boot auto-configuration with ShedLock distributed locking |

### Usage
Services inject `OutboxEventPublisher` and call `saveEvent(aggregateType, aggregateId, eventType, payload)`. The scheduled publisher handles reliable delivery to RabbitMQ with retry and idempotency guarantees.

### Migration Path
- **order-service**: Migrated from `R2dbcOutboxEventPublisher` to common implementation
- **inventory-service**: Replaced direct `RabbitTemplate` calls with `outboxPublisher.saveEvent()`
- **payment-service**: Adopts common publisher when implementing outbox pattern
- **category, notification**: Already used common publisher

---

## Idempotent Event Processing

### Problem
Duplicate `ReactiveIdempotentEventProcessor` implementations existed with different repository access patterns (`existsByEventId()` vs `findByEventId().isPresent()`), causing inconsistent duplicate detection behavior.

### Solution
Single `ReactiveIdempotentEventProcessor` in the `common` module supporting both patterns through a unified interface.

### Components

| Component | Location | Purpose |
|-----------|----------|---------|
| `ReactiveIdempotentEventProcessor` | `common/src/main/java/com/example/common/event/` | Reactive duplicate detection with configurable repository strategy |
| `IdempotentEventProcessor` | `common/src/main/java/com/example/common/event/` | Blocking version for legacy compatibility |
| `ProcessedEvent` / `CommonProcessedEventRepository` | `common/src/main/java/com/example/common/event/` | Persistence entities and repositories |

### Guarantees
- **At-least-once delivery** with **exactly-once processing** semantics
- Distributed locking via ShedLock prevents concurrent processing of same event
- Configurable TTL for processed event records

---

## Saga Orchestration

### Problem
Saga coordination for the order → inventory → payment → confirmation flow was distributed across service listeners with implicit ordering dependencies, making the flow difficult to understand, test, and recover from failures.

### Solution
Centralized `SagaOrchestrator` pattern with explicit state transitions, one per service boundary.

### Orchestrator Hierarchy

```
SagaOrchestrator (common interface)
├── OrderSagaOrchestratorImpl (order-service)
│   ├── createOrder() → publishes ORDER_CREATED
│   ├── handleInventoryReserved() → transitions order to RESERVED
│   ├── handlePaymentAuthorized() → delegates to PaymentProcessor
│   ├── handlePaymentCaptured() → transitions order to CONFIRMED
│   ├── handlePaymentFailed() → transitions order to CANCELLED
│   ├── handleReservationExpired() → compensates expired reservations
│   └── handleInventoryReleased() → compensates: transitions back to PENDING
├── InventorySagaOrchestratorImpl (inventory-service)
│   ├── handleOrderCreated() → reserves stock for all order items
│   └── handleOrderCancelled() → releases reservations
└── PaymentSagaOrchestratorImpl (payment-service)
    └── handleInventoryReserved() → authorizes payment using order/customer correlation
```

### State Transitions

| Event | Order Status | Inventory Status | Payment Status |
|-------|--------------|------------------|----------------|
| `ORDER_CREATED` | PENDING | — | — |
| `INVENTORY_RESERVED` (full) | RESERVED | RESERVED | — |
| `INVENTORY_RESERVED` (partial) | RESERVED | BACKORDERED | — |
| `INVENTORY_RESERVED` (none) | CANCELLED | CANCELLED | — |
| `PAYMENT_AUTHORIZED` | RESERVED | RESERVED | AUTHORIZED |
| `PAYMENT_CAPTURED` | CONFIRMED | CONFIRMED | CAPTURED |
| `PAYMENT_FAILED` | CANCELLED | RELEASED | FAILED |
| `RESERVATION_EXPIRED` | CANCELLED | RELEASED | — |

### Compensation Logic
- **Inventory release**: Transitions reserved items back to PENDING, order back to PENDING
- **Payment failure**: Releases inventory reservations, cancels order
- **Reservation expiry**: Scheduled job detects stale RESERVED orders (>30 min) and triggers compensation

### Scheduler
`ReservationExpiryScheduler` runs every 60 seconds, queries orders in RESERVED status with items reserved >30 minutes, and publishes `ReservationExpiredEvent` for each expired item.

---

## Service Decomposition

### OrderService Refactoring

**Before:** Single `OrderService` class (408 lines, ~15 public methods) handling:
- Order lifecycle (CRUD)
- Payment event processing
- Refund orchestration
- Status transitions
- Outbox publishing

**After:** Decomposed into focused modules:

| Module | Interface | Responsibility |
|--------|-----------|----------------|
| `OrderSagaOrchestrator` | `OrderSagaOrchestrator` | Saga coordination only |
| `PaymentProcessor` | `PaymentProcessor` | Payment operations (authorize, capture, refund) |
| `OrderEventPublisher` | `OrderEventPublisher` | Outbox event publishing |
| `OrderService` | — | Thin facade delegating to above |

### Benefits
- **Single Responsibility**: Each module has one reason to change
- **Testability**: Each module can be unit tested in isolation
- **Interface Segregation**: Consumers depend only on what they need
- **Depth Increase**: More behavior behind smaller interfaces

---

## Listener Abstraction

### Problem
Every event listener duplicated boilerplate: `@RabbitListener` annotation, idempotent processor invocation, reactive subscription with success/error logging.

### Solution
Base listener classes in the `common` module encapsulating the common pattern.

### Base Classes

```java
// Blocking version
public abstract class BaseSagaListener<E extends BaseEvent> {
    protected BaseSagaListener(IdempotentEventProcessor processor)
    protected final void processEvent(E event)
    protected abstract void handleEventInternal(E event)
}

// Reactive version
public abstract class BaseReactiveSagaListener<E extends BaseEvent> {
    protected BaseReactiveSagaListener(ReactiveIdempotentEventProcessor processor)
    protected final void processEvent(E event)
    protected abstract Mono<Void> handleEventInternal(E event)
}
```

### Usage
Concrete listeners extend the base class and implement only `handleEventInternal()`:

```java
@Component
public class InventoryEventListener extends BaseReactiveSagaListener<InventoryEvent> {
    
    public InventoryEventListener(ReactiveIdempotentEventProcessor processor,
                                   OrderSagaOrchestrator orchestrator) {
        super(processor);
        this.orchestrator = orchestrator;
    }

    @RabbitListener(queues = "${rabbitmq.queue.inventory-events}")
    public void handleInventoryEvent(InventoryEvent event) {
        processEvent(event);
    }

    @Override
    protected Mono<Void> handleEventInternal(InventoryEvent event) {
        return switch (event.getEventType()) {
            case "RESERVED" -> orchestrator.handleInventoryReserved(event);
            case "RELEASED" -> orchestrator.handleInventoryReleased(event);
            default -> Mono.empty();
        };
    }
}
```

### Adoption
All services now use base listeners:
- **order-service**: `InventoryEventListener`, `PaymentEventListener`, `ReservationExpiredEventListener`
- **inventory-service**: `InventoryEventListener`
- **payment-service**: `InventoryEventListener`

---

## Cross-Cutting Concerns

### Event Correlation
`InventoryEvent` enhanced with correlation fields:
```java
private Long orderId;        // Links to originating order
private String customerId;   // Customer identifier
private String customerEmail; // Customer contact
```

Enables payment-service to authorize payments using actual customer context rather than variant-derived identifiers.

### Distributed Locking
ShedLock integrated into outbox publisher for:
- Preventing duplicate event publishing across service instances
- Ensuring exactly-once semantics in clustered deployments
- Configurable lock TTL and retry policies

### Error Handling
- All exceptions propagate through reactive pipelines (no silent failures)
- Circuit breakers (Resilience4j) on external calls (payment gateway)
- Dead letter handling via RabbitMQ DLX for unprocessable events

---

## Testing and Verification

### Test Strategy

| Test Type | Coverage |
|-----------|----------|
| Unit | All orchestrator logic, state transitions, compensation paths |
| Integration | Event listener → orchestrator delegation, outbox publishing |
| Contract | Pact consumer/provider tests for service boundaries |
| Architecture | ArchUnit rules enforcing module boundaries |

### Verification Commands
```bash
# All tests (excluding system tests requiring full infra)
mvn test -pl '!system-test'

# Full build
mvn clean install -DskipTests -pl '!system-test'
```

### Results
All modules pass:
- common: 14 tests
- order-service: 52 tests
- inventory-service: 13 tests
- payment-service: passes
- notification-service: 5 tests
- product-service: passes
- category-service: passes
- architecture-tests: 5 tests

---

## Module Dependencies

```
common (event infrastructure)
├── OutboxEventPublisher
├── ReactiveIdempotentEventProcessor
├── SagaOrchestrator (interface)
└── Base*Listener (abstract classes)

order-service
├── OrderSagaOrchestratorImpl
├── PaymentProcessorImpl
├── OrderEventPublisherImpl
└── ReservationExpiryScheduler

inventory-service
├── InventoryService (uses OutboxEventPublisher)
└── InventorySagaOrchestratorImpl

payment-service
├── PaymentService
└── PaymentSagaOrchestratorImpl

category, notification, product
└── Use common implementations directly
```

---

## Configuration

### Outbox Publisher
```yaml
outbox:
  publisher:
    batch-size: 100
    poll-interval: 5000ms
    lock-provider: shedlock
    retry:
      max-attempts: 5
      backoff: 2000ms
```

### Saga Timeouts
```yaml
saga:
  reservation-ttl: 30min
  expiry-check-interval: 60s
  payment-authorization-timeout: 10s
```

### Idempotency
```yaml
idempotency:
  ttl: 24h
  cleanup-interval: 1h
```

---

## References

- [Domain Model](../domain-model.md) — Core domain terminology
- [Service Ports](../../AGENTS.md#service-ports-default) — Port assignments
- [ADR 001: Outbox Pattern](../adr/001-outbox-pattern.md)
- [ADR 002: Saga Orchestration](../adr/002-saga-orchestration.md)
- [ADR 003: Common Module Strategy](../adr/003-common-module.md)