# 12 — Product: MongoDB Configuration & Indexes

**What to build:** Spring configuration for reactive MongoDB client: connection URI, database name, auto-index creation. Compound indexes for query performance.

**Blocked by:** 06 — Product: POM Cleanup

**Status:** ready-for-agent

- [x] Add `spring.data.mongodb.uri`, `spring.data.mongodb.database`, `spring.data.mongodb.auto-index-creation=true` to `product/src/main/resources/application.yml`
- [ ] Add `@Configuration` class for custom `MongoClientSettings` if needed (timeouts, connection pool)
- [x] Ensure indexes created on startup: compound index on `categoryId`, text index on `name`/`description`, index on `variants.attributes` (via @CompoundIndex on entity)
- [ ] Verify service starts and connects to MongoDB (Testcontainers in integration test)
- [ ] Verify `mvn verify -pl product` runs integration tests against Testcontainers MongoDB