# 08 — Outbox Publisher Leader Election

**What to build:** Add leader election to `OutboxEventPublisher` so only one instance processes the outbox at a time, preventing duplicate event publishing when multiple service instances run.

**Blocked by:** None — can start immediately.

**Status:** done

- [x] Add ShedLock dependency: `net.javacrumbs.shedlock:shedlock-spring` and provider (e.g., `shedlock-provider-jdbc-template` for PostgreSQL)
- [x] Configure `LockProvider` bean using DataSource
- [x] Annotate `publishOutboxEvents()` with `@SchedulerLock(name = "outboxPublisher", lockAtLeastFor = "30s", lockAtMostFor = "5m")`
- [x] Or implement custom leader election using PostgreSQL advisory locks or Consul/Zookeeper if available
- [x] Alternative: Deploy outbox publisher as a single-instance separate worker service
- [x] Verify: Run multiple instances of a service with outbox publisher; confirm only one processes events
- [x] Run tests: `mvn test -pl common,order-service,payment-service,inventory-service`