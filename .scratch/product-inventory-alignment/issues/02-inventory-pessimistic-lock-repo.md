# 02 — Inventory: Pessimistic Lock Repository Method

**What to build:** `InventoryRepository` exposes `findByVariantIdWithLock(Long variantId)` that acquires a `SELECT ... FOR UPDATE` row lock via `@Lock(PESSIMISTIC_WRITE)`. All existing query methods preserved.

**Blocked by:** 01 — Inventory: POM Cleanup

**Status:** ready-for-agent

- [ ] Add `findByVariantIdWithLock(@Param("variantId") Long variantId)` to `InventoryRepository`
- [ ] Annotate with `@Lock(LockModeType.PESSIMISTIC_WRITE)` and `@Query("SELECT i FROM Inventory i WHERE i.variantId = :variantId")`
- [ ] Keep existing methods: `findByVariantId`, `findByProductId`, `findLowStockItems`, `findStaleReservations`
- [ ] Verify `mvn compile -pl inventory-service` succeeds
- [ ] Unit test: mock repository, verify lock method called with correct variantId