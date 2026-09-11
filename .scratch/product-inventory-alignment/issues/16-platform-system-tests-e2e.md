# 16 — Platform: System Tests E2E Validation

**What to build:** Updated `system-test` module runs full E2E flow (browse → order → pay → confirm) with MongoDB for product + PostgreSQL for inventory. All existing tests pass; new tests verify reactive product endpoints.

**Blocked by:** 05 — Inventory: Concurrency Integration Test, 10 — Product: Reactive Controller, 14 — Platform: Docker Compose MongoDB Service, 15 — Platform: Config Server MongoDB Config

**Status:** done

- [x] Update `system-test/pom.xml`: add MongoDB Testcontainers dependency
- [x] Update `docker-compose.test.yml` to include MongoDB (from ticket 14)
- [x] Update `TestcontainersConfig` to include MongoDB container
- [x] Update `ECommerceSystemTest` to use MongoDB for product service (DynamicPropertySource)
- [x] Update `DatabaseTestHelper` to use MongoDB for product test data (createProduct, findProductById, findProductVariantById)
- [x] Verify existing happy-path test: browse products → create order → reserve inventory → authorize payment → confirm order
- [x] Add test: product search/filter via reactive endpoints
- [x] Add test: concurrent order placement → inventory concurrency (ticket 05) prevents oversell
- [x] Verify all tests pass: `mvn verify -pl system-test`
- [x] CI pipeline runs system tests on every PR