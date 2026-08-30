# 07 — Order–Payment Integration: PENDING ↔ CONFIRMED/CANCELLED

**What to build:** `order-service` consumes `PaymentEvent.SUCCESS` → `PENDING → CONFIRMED`; consumes `PaymentEvent.FAILED` → `PENDING → CANCELLED` + triggers inventory release via `OrderEvent.CANCELLED`. Idempotent via `processed_events`.

**Blocked by:** 04a — Order Management: OrderItem Aggregate (Core) and 06b — Payment Service: Auth/Capture + Refunds + PaymentEvent

**Status:** ready-for-agent

- [ ] Add `PaymentEventListener` in `order-service` (or reuse `OrderEventListener`):
  - Consume `PaymentEvent` via RabbitMQ/Kafka (Debezium sink)
  - Idempotency check via `processed_events` table (from Ticket 01)
  - On `PaymentEvent.SUCCESS` (status = CAPTURED):
    - Find Order by `orderId`
    - Transition Order status: `PENDING → CONFIRMED`
    - Update OrderItems status: `PENDING → RESERVED` (or SHIPPED if already shipped)
    - Write `OrderEvent.UPDATED` (status=CONFIRMED) to outbox
  - On `PaymentEvent.FAILED` or `REFUNDED` (before capture):
    - Find Order by `orderId`
    - Transition Order status: `PENDING → CANCELLED`
    - Update OrderItems status: `PENDING → CANCELLED`
    - Write `OrderEvent.CANCELLED` to outbox (triggers inventory release via existing listener)
- [ ] Handle `PaymentEvent.PARTIALLY_REFUNDED` → if Order not yet shipped, adjust OrderItem quantities/status
- [ ] Update `OrderEvent` in `common`: ensure `eventId` present for idempotency
- [ ] Integration tests:
  - Payment success → order CONFIRMED + items RESERVED
  - Payment failure → order CANCELLED + inventory released
  - Duplicate PaymentEvent → processed once (idempotency)
  - Payment after order already shipped → no status regression