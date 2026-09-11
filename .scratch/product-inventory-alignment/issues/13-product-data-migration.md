# 13 — Product: Data Migration Script + Validation Test

**What to build:** One-time migration script reading from PostgreSQL `product` + `product_variant` tables → writing MongoDB `Product` documents with embedded variants. Validation test confirming document count, variant embedding, query correctness.

**Blocked by:** 07 — Product: MongoDB Document Model, 08 — Product: Reactive Repository, 12 — Product: MongoDB Configuration

**Status:** done

- [x] Create `ProductMigrationRunner` implementing `CommandLineRunner` (or standalone main class)
- [x] Read all products from PostgreSQL via `JdbcTemplate` (temporary dependency) or separate migration module
- [x] For each product, fetch variants; build `Product` document with embedded `ProductVariant` list
- [x] Write to MongoDB via `ReactiveMongoTemplate` or blocking `MongoTemplate` (using `productRepository.save().block()`)
- [x] Log progress: total products, variants migrated, errors
- [ ] Make migration idempotent (upsert by `id` or `skuCode`)
- [ ] Integration test: run migration → verify MongoDB document count = PostgreSQL product count
- [ ] Verify embedded variants: each document has correct variant count, SKU codes, attributes, prices
- [ ] Verify queries work: find by category, text search, attribute filter
- [ ] Document rollback: keep PostgreSQL read-only during cutover; revert = redeploy old version
