# 05b — Inventory: TTL Scheduler (15 min) + LOW_STOCK

**What to build:** Scheduler `@Scheduled(1min)` scans `OrderItem` where `status = RESERVED` and `reservedAt < now - 15 min` → releases reservation by calling `releaseReservation`. `LOW_STOCK` event fires on `availableQuantity ≤ reorderLevel`.

**Blocked by:** 05a — Inventory: Reservation Core (Reserve/Confirm/Release)

**Status:** done

- [x] Add `@EnableScheduling` to `inventory-service` configuration
- [x] Implement `ReservationExpiryScheduler`:
  - `@Scheduled(fixedDelay = 60000)` (1 minute)
  - Query: `SELECT r FROM Reservation r WHERE r.status = 'RESERVED' AND r.reservedAt < :cutoff` (cutoff = now - 15 min)
  - For each: call `releaseReservation(i.getVariantId(), i.getQuantityOrdered())` which publishes `ReservationExpiredEvent`
  - Use DB advisory lock (`pg_advisory_lock`) to prevent multiple instances processing same reservation
- [x] Implement `LOW_STOCK` detection:
  - After any `quantity` or `reservedQuantity` change, check `availableQuantity <= reorderLevel`
  - If crossed threshold (was above, now below), publish `InventoryEvent.LOW_STOCK` directly to RabbitMQ
  - Avoid duplicate events: track last notified level or use boolean flag `lowStockNotified`
- [x] Add `reorderLevel` configuration per variant (default 10, overridable)
- [x] Integration tests:
  - Create reservation, manipulate `reservedAt` to > 16 min ago, verify auto-release + `ReservationExpiredEvent` + `InventoryEvent.RELEASED`
  - Reduce stock to trigger `LOW_STOCK`, verify event published once
  - Multiple scheduler instances: verify single execution via advisory lock
