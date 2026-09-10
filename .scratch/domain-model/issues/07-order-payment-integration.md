# 07 — Order–Payment Integration: PENDING ↔ CONFIRMED/CANCELLED

**What to build:** `order-service` consumes `PaymentEvent.SUCCESS` → `PENDING → CONFIRMED`; consumes `PaymentEvent.FAILED` → `PENDING → CANCELLED` + triggers inventory release via `OrderEvent.CANCELLED`. Idempotent via `processed_events`.

**Blocked by:** 04a — Order Management: OrderItem Aggregate (Core) and 06b — Payment Service: Auth/Capture + Refunds + PaymentEvent

**Status:** **DONE**

- [x] Add `PaymentEventListener` in `order-service`:
  - Consume `PaymentEvent` via RabbitMQ
  - Idempotency check via `processed_events` table (from Ticket 01)
  - On `PaymentEvent.CAPTURED`:
    - Find Order by `orderId`
    - Transition Order status: `PENDING → CONFIRMED`
    - Update OrderItems status: `PENDING → RESERVED`
    - Write `OrderEvent.UPDATED` (status=CONFIRMED) to RabbitMQ
  - On `PaymentEvent.FAILED`:
    - Find Order by `orderId`
    - Transition Order status: `PENDING → CANCELLED`
    - Update OrderItems status: `PENDING → CANCELLED`
    - Write `OrderEvent.CANCELLED` to RabbitMQ (triggers inventory release)
  - On `PaymentEvent.REFUNDED` / `PARTIALLY_REFUNDED`:
    - Handle full/partial refund logic
  - On `PaymentEvent.AUTHORIZED`:
    - Log but no state change needed
- [x] Update `OrderEvent` in `common`: ensure `eventId` present for idempotency
- [x] Integration tests:
  - Payment success → order CONFIRMED + items RESERVED
  - Payment failure → order CANCELLED + inventory released
  - Duplicate PaymentEvent → processed once (idempotency)
  - Payment after order already shipped → no status regression