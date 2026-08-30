# 09c — System Tests: High Throughput / Load

**What to build:** (1) Concurrent order placement (100+ parallel) — verify no oversell; (2) Concurrent reservation + payment capture — verify idempotency under load; (3) Debezium lag measurement under sustained write load; (4) Scheduler contention — multiple inventory-service instances, verify single expiry execution via advisory lock.

**Blocked by:** 09a — System Tests: Infrastructure + Happy Path

**Status:** ready-for-agent

- [ ] Test: **Concurrent Order Placement (No Oversell)**
  - Create Inventory with quantity=100 for a variant
  - Launch 200 parallel requests to `POST /orders` (each ordering 1 unit)
  - Verify exactly 100 orders succeed (CONFIRMED), 100 fail (insufficient stock)
  - Verify final inventory: quantity=0, reservedQuantity=0, availableQuantity=0
  - No negative quantities, no lost updates
- [ ] Test: **Concurrent Reservation + Payment Capture**
  - Place 50 orders in parallel (each reserves 1 unit)
  - Immediately trigger payment capture for all 50 in parallel
  - Verify all 50 capture successfully (idempotent)
  - Verify inventory: 50 confirmed (quantity reduced), 0 reserved
  - No duplicate confirmations, no race conditions
- [ ] Test: **Debezium Lag Under Sustained Load**
  - Run sustained write load: 100 orders/minute for 10 minutes
  - Monitor Debezium connector lag (via Kafka consumer group lag or Debezium metrics)
  - Verify lag stays bounded (< 1000 events) — no unbounded growth
  - Verify no event loss (count events in Kafka = count in outbox)
- [ ] Test: **Scheduler Contention (Multiple Inventory Instances)**
  - Start 3 `inventory-service` instances (same DB)
  - Create 50 stale reservations (> 15 min old)
  - Verify exactly ONE instance processes each expiry (advisory lock)
  - Total `RELEASED` events = 50 (not 150)
  - Verify no deadlocks, no lock timeouts
- [ ] Add load test utilities to `system-test`: parallel executor, metrics collector, assertion helpers
- [ ] Document: how to run load tests locally (resource requirements), CI integration (nightly)