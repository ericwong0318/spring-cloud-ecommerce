# 03 — Inventory: Service Mutations Under Pessimistic Lock

**What to build:** `InventoryService.reserveStock()`, `releaseReservation()`, `confirmStock()` all acquire row lock via `findByVariantIdWithLock()` inside `@Transactional`. No oversell possible under concurrent load. Reactive types (`Mono`/`Flux`) removed.

**Blocked by:** 02 — Inventory: Pessimistic Lock Repository Method

**Status:** ready-for-agent

- [x] Update `reserveStock()`: call `findByVariantIdWithLock(variantId)` at start of transaction; apply reservation logic on locked entity
- [x] Update `releaseReservation()`: same lock pattern; decrement `reservedQuantity`
- [x] Update `confirmStock()`: same lock pattern; decrement both `quantity` and `reservedQuantity`
- [x] Remove all reactive return types (`Mono`/`Flux`) — methods return plain `ReservationResult` / `void`
- [x] Verify `mvn compile -pl inventory-service` succeeds
- [x] Unit tests: mock repository, verify lock method called; test full/partial/backorder reservation logic
