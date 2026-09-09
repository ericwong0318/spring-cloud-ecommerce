# 05 — Inventory: Concurrency Integration Test

**What to build:** Integration test proving no oversell under concurrent load. Spawns 100 threads calling `reserveStock` for same variant simultaneously; asserts total reserved ≤ available quantity. Runs with Testcontainers PostgreSQL.

**Blocked by:** 04 — Inventory: MVC Controller

**Status:** ready-for-agent

- [ ] Create `InventoryConcurrencyIntegrationTest` using `@SpringBootTest` + Testcontainers PostgreSQL
- [ ] Initialize variant with `quantity=100`, `reservedQuantity=0`
- [ ] Spawn 100 threads via `ExecutorService`, each calling `reserveStock(variantId, 1, orderItemId)`
- [ ] Use `CountDownLatch` to synchronize concurrent start
- [ ] Assert: sum of all successful reservations ≤ 100; no exceptions from oversell
- [ ] Verify `reservedQuantity` in DB matches successful count
- [ ] Verify `inventory.stock_reserved` events published for each successful reservation
- [ ] Test runs in < 30 seconds; suitable for CI