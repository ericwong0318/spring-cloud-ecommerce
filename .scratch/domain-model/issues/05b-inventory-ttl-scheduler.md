# 05b — Inventory: TTL Scheduler (15 min) + LOW_STOCK

**What to build:** Scheduler `@Scheduled(1min)` scans `Inventory.updatedAt` > 15 min → `releaseReservation`. `LOW_STOCK` event fires on `availableQuantity ≤ reorderLevel`.

**Blocked by:** 05a — Inventory: Reservation Core (Reserve/Confirm/Release)

**Status:** ready-for-agent

- [ ] Add `@EnableScheduling` to `inventory-service` configuration
- [ ] Implement `ReservationExpiryScheduler`:
  - `@Scheduled(fixedDelay = 60000)` (1 minute)
  - Query: `SELECT i FROM Inventory i WHERE i.reservedQuantity > 0 AND i.updatedAt < :cutoff` (cutoff = now - 15 min)
  - For each: call `releaseReservation(inventory.getVariantId(), inventory.getReservedQuantity())`
  - Use DB advisory lock (`pg_advisory_lock`) to prevent multiple instances processing same reservation
- [ ] Implement `LOW_STOCK` detection:
  - After any `quantity` or `reservedQuantity` change, check `availableQuantity <= reorderLevel`
  - If crossed threshold (was above, now below), write `InventoryEvent.LOW_STOCK` to outbox
  - Avoid duplicate events: track last notified level or use boolean flag `lowStockNotified`
- [ ] Add `reorderLevel` configuration per variant (default 10, overridable)
- [ ] Integration tests:
  - Create reservation, wait 16 min (or manipulate `updatedAt`), verify auto-release + `RELEASED` event
  - Reduce stock to trigger `LOW_STOCK`, verify event published once
  - Multiple scheduler instances: verify single execution via advisory lock