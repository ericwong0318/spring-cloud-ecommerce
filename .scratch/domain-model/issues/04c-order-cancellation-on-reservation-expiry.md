# 04c — Order Service: ReservationExpiredEvent Handler (CANCELLED on timeout)

**What to build:** order-service consumes `ReservationExpiredEvent` from RabbitMQ. When a reservation expires, the matching `OrderItem.status` is set to CANCELLED, and if all OrderItems are CANCELLED/BACKORDERED, the parent `Order` is transitioned to CANCELLED. Idempotent via `processed_events` table (ticket #01).

**Blocked by:** 04a — Order Management: OrderItem Aggregate (Core), 05b — Inventory: TTL Scheduler

**Status:** ready-for-agent

- [ ] Create `ReservationExpiredEvent` in `common` module: `{eventId, orderItemId, variantId, quantityReleased, reservationExpiresAt}`
- [ ] Implement `ReservationExpiredEventHandler` in `order-service`:
  - Listen on RabbitMQ queue `order.reservation-expired.queue`
  - Idempotency check via `IdempotentEventProcessor` (ticket #01)
  - Find OrderItem by ID, set `status = CANCELLED`
  - Check parent Order: if all OrderItems are CANCELLED or BACKORDERED, transition `Order.status = CANCELLED`
  - Publish `OrderEvent.CANCELLED` to RabbitMQ (so notification-service can notify customer)
- [ ] Add integration tests:
  - Publish `ReservationExpiredEvent` → verify OrderItem.status = CANCELLED, Order.status = CANCELLED if all items expired
  - Partial expiry: 2 of 3 items expired → Order remains in PENDING (not all items CANCELLED yet)
  - Duplicate event (same `eventId`) → handled once (idempotency)
  - OrderItem already CANCELLED → handler is a no-op (idempotent)

## Key Decisions (from grilling session)

- **Inventory publishes the event** (separation of concerns) — order-service reacts
- **Order owns the transition** — only Order can change its own status
- **No PaymentEvent on cancel** — payment-service independently consumes `OrderEvent.CANCELLED` if needed for refund (separate ticket)