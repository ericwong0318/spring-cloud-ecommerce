# 16 — Platform: System Tests E2E Validation

**What to build:** Updated `system-test` module runs full E2E flow (browse → order → pay → confirm) with MongoDB for product + PostgreSQL for inventory. All existing tests pass; new tests verify reactive product endpoints.

**Blocked by:** 05 — Inventory: Concurrency Integration Test, 10 — Product: Reactive Controller, 14 — Platform: Docker Compose MongoDB Service, 15 — Platform: Config Server MongoDB Config

**Status:** ready-for-agent

- [ ] Update `system-test/pom.xml`: add MongoDB Testcontainers dependency
- [ ] Update `docker-compose.test.yml` to include MongoDB (from ticket 14)
- [ ] Update `ECommerceSystemTest` to use MongoDB for product service
- [ ] Verify existing happy-path test: browse products → create order → reserve inventory → authorize payment → confirm order
- [ ] Add test: product search/filter via reactive endpoints
- [ ] Add test: concurrent order placement → inventory concurrency (ticket 05) prevents oversell
- [ ] Verify all tests pass: `mvn verify -pl system-test`
- [ ] CI pipeline runs system tests on every PR