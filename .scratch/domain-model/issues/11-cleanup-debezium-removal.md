# cleanup — Remove Debezium CDC Artifacts

**What to build:** Remove all Debezium CDC infrastructure (docker, POM, SQL, scripts) since the project uses **direct RabbitMQ publishing** with publisher confirms (ADR-006). After cleanup, docker-compose starts without Debezium, Maven builds without Debezium BOM, and all tickets reference RabbitMQ-only event flow.

**Blocked by:** None — can start immediately.

**Status:** ready-for-agent

- [ ] Remove `debezium-connect` service from `docker-compose.yml` (lines 74-102)
- [ ] Remove `wal_level=logical` PostgreSQL comment from `docker-compose.yml` (line 381)
- [ ] Remove `<debezium.version>` property from root `pom.xml` (line 42)
- [ ] Remove Debezium BOM `dependencyManagement` block from root `pom.xml` (lines 115-119)
- [ ] Remove commented `CREATE PUBLICATION debezium_outbox` from `order-service` migration SQL
- [ ] Remove commented `CREATE PUBLICATION debezium_outbox` from `inventory-service` migration SQL
- [ ] Remove commented `CREATE PUBLICATION debezium_outbox` from `notification-service` migration SQL
- [ ] Delete `scripts/register-debezium-connectors.sh`
- [ ] Update `08-notifications.md`: remove "02 — Debezium CDC Setup for Event Publishing" from Blocked by
- [ ] Update `09a-system-tests-happy-path.md`: remove Debezium container from Testcontainers config; remove "Debezium CDC Verification" test
- [ ] Update `09c-system-tests-load.md`: remove "Debezium Lag Under Sustained Load" test
- [ ] Update `03b-category-hierarchy.md`: remove "Write category changes to outbox table (Debezium)" task
- [ ] Update `06a-payment-scaffold.md`: remove Debezium from dependencies list
- [ ] Search codebase for `debezium` / `Debezium` / `CDC` — verify only historical comments remain
- [ ] Verify `docker-compose up -d` starts without Debezium
- [ ] Verify `mvn clean install -DskipTests` passes
