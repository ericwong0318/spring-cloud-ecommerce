# 08 — Outbox Publisher Leader Election

**What to build:** Add leader election to `OutboxEventPublisher` so only one instance processes the outbox at a time, preventing duplicate event publishing when multiple service instances run.

**Blocked by:** None — can start immediately.

**Status:** ready-for-agent

- [ ] Add ShedLock dependency: `net.javacrumbs.shedlock:shedlock-spring` and provider (e.g., `shedlock-provider-jdbc-template` for PostgreSQL)
- [ ] Configure `LockProvider` bean using DataSource
- [ ] Annotate `publishOutboxEvents()` with `@SchedulerLock(name = "outboxPublisher", lockAtLeastFor = "30s", lockAtMostFor = "5m")`
- [ ] Or implement custom leader election using PostgreSQL advisory locks or Consul/Zookeeper if available
- [ ] Alternative: Deploy outbox publisher as a single-instance separate worker service
- [ ] Verify: Run multiple instances of a service with outbox publisher; confirm only one processes events
- [ ] Run tests: `mvn test -pl common,order-service,payment-service,inventory-service`