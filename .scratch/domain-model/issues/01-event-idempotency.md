# 01 — Event Infrastructure: `eventId` + Idempotency Foundation

**What to build:** All integration events (`ProductEvent`, `OrderEvent`, `InventoryEvent`, `PaymentEvent`) carry a UUID `eventId`. Each consumer service has a `processed_events(event_id PK, processed_at)` table + a reusable idempotency filter that deduplicates at-least-once RabbitMQ delivery. Backward-compatible deploy.

**Blocked by:** None — can start immediately.

**Status:** ready-for-agent

- [x] Add `eventId: UUID` field to all event classes in `common` module
- [x] Create `ProcessedEvent` JPA entity (`eventId` PK, `processedAt`)
- [x] Create `ProcessedEventRepository` per service (or shared in `common`)
- [x] Implement `IdempotentEventListener` base class / filter: check `processed_events` before handling; insert after success
- [x] Update `OrderEvent`, `InventoryEvent`, `ProductEvent`, `PaymentEvent` with `eventId` generation in factory methods
- [x] Add integration test: publish duplicate event → verify processed once
- [x] Verify backward compatibility: consumers without `eventId` handling still work (optional field)