# 01 — Unify Outbox Event Publisher in common module

**What to build:** Consolidate the duplicated outbox event publisher implementations into a single shared implementation in the common module. The common module already has `ReactiveOutboxEventPublisher` and `OutboxEventRepository`, but the order-service has its own `R2dbcOutboxEventPublisher` with nearly identical logic (payload serialization, routing key determination, RabbitMQ publishing). The common version uses ShedLock for distributed locking; the order-service version does not. This ticket creates a unified `OutboxEventPublisher` interface in common and makes all services depend on it.

**Blocked by:** None — can start immediately.

**Acceptance criteria:**
- [x] Define `OutboxEventPublisher` interface in `common/src/main/java/com/example/common/event/` with `publishEvent()` and `saveEvent()` methods
- [x] Move shared publishing logic from `R2dbcOutboxEventPublisher` (order-service) into the common implementation
- [x] Ensure distributed locking via ShedLock is consistent across all services
- [x] Update `order-service`, `payment-service`, and `inventory-service` to depend on the common `OutboxEventPublisher` instead of their own implementations
- [x] Run `mvn test` across all affected modules to verify no regressions

**Note:** payment-service and inventory-service don't have outbox publishers yet — they will use the common one when they implement the outbox pattern. order-service has been migrated. category and notification-service already used the common publisher.