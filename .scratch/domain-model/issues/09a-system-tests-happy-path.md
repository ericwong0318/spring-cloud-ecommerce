# 09a — System Tests: Infrastructure + Happy Path

**What to build:** Testcontainers suite: (1) Full flow: Order → reserve → auth → capture → confirm → partial ship → deliver; (2) Debezium CDC verification — events appear in Kafka/RabbitMQ; (3) Idempotency — duplicate `OrderEvent.CREATED` processed once.

**Blocked by:** 04b — Order Management: Shipment Aggregate, 05b — Inventory: TTL Scheduler (15 min) + LOW_STOCK, 07 — Order–Payment Integration: PENDING ↔ CONFIRMED/CANCELLED, 08 — Notifications: Templates + Fixed Retry (3×5min) + Email→SMS Fallback

**Status:** ready-for-agent

- [ ] Create `system-test` module Testcontainers configuration: PostgreSQL (per service or shared), Kafka/RabbitMQ, Debezium container
- [ ] Implement test infrastructure:
  - `TestcontainersConfig` with `@Container` for PostgreSQL, Kafka, Debezium
  - `EventCollector` utility: subscribes to event topics, collects events for assertions
  - `DatabaseTestHelper` per service: inserts test data, verifies state
- [ ] Test: **Full Happy Path**
  1. Create Product + Variants via `product-service` API
  2. Create Inventory for variants via `inventory-service` API
  3. Place Order via `order-service` API (multiple lines, different variants)
  4. Verify `OrderEvent.CREATED` received by inventory-service → reservations created
  5. Authorize + Capture payment via `payment-service` API
  6. Verify `PaymentEvent.SUCCESS` → `order-service` transitions PENDING → CONFIRMED
  7. Create Shipment (partial) via `order-service` API
  8. Verify `OrderEvent.SHIPPED` + `quantityShipped` updated
  9. Complete shipment → DELIVERED
  10. Verify notifications sent at each stage (order confirmation, payment success, shipment)
- [ ] Test: **Debezium CDC Verification**
  - Write to outbox table directly → verify event appears in Kafka topic with correct schema
  - Verify `eventId` present, payload matches domain change
- [ ] Test: **Idempotency**
  - Publish duplicate `OrderEvent.CREATED` (same `eventId`) → verify inventory reservation only once
  - Publish duplicate `PaymentEvent.SUCCESS` → verify order transition only once