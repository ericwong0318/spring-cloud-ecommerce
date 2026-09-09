# Product & Inventory Architecture Alignment — Spec

## Problem Statement

The current platform specification defines Product Service as WebFlux + JPA (PostgreSQL) and Inventory Service as WebFlux + R2DBC (PostgreSQL). The recommended e-commerce architecture specifies:
- **Product/Read**: WebFlux + MongoDB/Elasticsearch for fast, non-blocking page rendering and flexible search
- **Inventory/Stock**: JPA (MVC) + PostgreSQL with pessimistic/optimistic locking to prevent overselling

The current implementation diverges from this recommendation. Product uses blocking JPA; Inventory uses a confusing hybrid of WebFlux + JPA + R2DBC. This creates operational complexity and doesn't match the access patterns each domain requires.

## Solution

Re-architect the two services to match the recommended stack:

1. **Product Service** → **WebFlux + MongoDB** (reactive, document-based)
   - Move from relational JPA to MongoDB reactive repositories
   - Embed ProductVariant in Product document for single-read catalog access
   - Expose reactive REST endpoints (`Flux`/`Mono`)
   - Keep MapStruct for DTO mapping where needed

2. **Inventory Service** → **Spring MVC (WebMVC) + JPA + PostgreSQL** with **pessimistic locking**
   - Remove WebFlux and R2DBC dependencies entirely
   - Use `@Lock(PESSIMISTIC_WRITE)` on stock mutation queries (`SELECT FOR UPDATE`)
   - Reserve/release/confirm operations execute under serializable isolation via row locks
   - Keep RabbitMQ event publishing for downstream consumers

## User Stories

### Product Service (MongoDB Migration)

1. As a shopper, I want to browse products with sub-100ms latency, so that the catalog feels responsive.
2. As a shopper, I want to filter products by category, price range, and attributes (size, color), so that I can narrow results quickly.
3. As a shopper, I want full-text search on product name and description, so that I can find items without exact names.
4. As a merchant, I want to create a product with multiple variants (SKU per size/color) in a single request, so that catalog management is efficient.
5. As a merchant, I want to update variant price or attributes independently, so that I don't reload the entire product.
6. As a system, I want product reads to scale horizontally without connection pooling contention, so that traffic spikes don't degrade performance.
7. As a developer, I want the product data model to accommodate dynamic attributes without schema migrations, so that new variant properties don't require DDL.

### Inventory Service (Pessimistic Locking)

8. As a system, I want to reserve stock for an order line item with a guarantee that no concurrent request can oversell the same units, so that inventory accuracy is absolute.
9. As a system, I want to release a reservation when an order is cancelled or expires, so that stock returns to available pool.
10. As a system, I want to confirm stock reduction when an order ships, so that physical inventory matches reserved state.
11. As an operator, I want to see available vs. reserved vs. on-hand quantities in real time, so that I can make replenishment decisions.
12. As a system, I want to publish `inventory.stock_reserved`, `inventory.stock_released`, `inventory.stock_confirmed` events to RabbitMQ, so that Order and Payment services react reliably.
13. As a system, I want to detect low-stock conditions and publish `inventory.low_stock` events, so that purchasing is alerted.

### Cross-Service Integration

14. As a developer, I want the Gateway routes (`lb://product`, `lb://inventory-service`) to remain unchanged, so that consumers don't need updates.
15. As a developer, I want the `common` module DTOs (`ProductDto`, `ProductVariantDto`, `InventoryDto`) to stay stable, so that other services don't break.
16. As a system, I want the saga choreography (Order → Inventory → Payment) to function identically, so that end-to-end tests pass without modification.

## Implementation Decisions

### Product Service — MongoDB Migration

| Decision | Detail |
|----------|--------|
| **Stack** | `spring-boot-starter-webflux`, `spring-boot-starter-data-mongodb-reactive` |
| **Removed** | `spring-boot-starter-data-jpa`, `postgresql`, `flyway-core`, `spring-boot-starter-web` |
| **Data Model** | Single `Product` document with embedded `variants` array. Each variant: `skuCode`, `attributes` (Map<String,String>), `price`, `inventoryId` (reference to Inventory service). |
| **Repository** | `ReactiveMongoRepository<Product, String>` with custom query methods for category, text search, attribute filters. |
| **Indexes** | Compound index on `categoryId`, `name` (text), `variants.attributes` (for filter queries). |
| **Controller** | `@RestController` returning `Flux<ProductDto>` / `Mono<ProductDto>`. Endpoints: `GET /api/products`, `GET /api/products/{id}`, `GET /api/products/search`, `POST /api/products`, `PUT /api/products/{id}`, `DELETE /api/products/{id}`. |
| **Service** | Reactive service methods returning `Mono`/`Flux`. No `@Transactional` (MongoDB transactions only if needed for multi-document). |
| **Mapping** | MapStruct mappers updated for document ↔ DTO. Optionally remove mappers and map directly in service. |
| **Configuration** | `spring.data.mongodb.uri`, `database`, `auto-index-creation=true`. Reactive MongoClient auto-configured. |
| **Migration** | One-time script: read from PostgreSQL `product` + `product_variant` tables → write MongoDB documents. Run before cutover. |

### Inventory Service — JPA + Pessimistic Locking

| Decision | Detail |
|----------|--------|
| **Stack** | `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `postgresql` (keep existing). |
| **Removed** | `spring-boot-starter-webflux`, `spring-boot-starter-data-r2dbc`, `r2dbc-postgresql`, `springdoc-openapi-starter-webflux-ui` (replace with `webmvc-ui`). |
| **Locking Strategy** | `InventoryRepository.findByVariantIdWithLock()` annotated with `@Lock(LockModeType.PESSIMISTIC_WRITE)` → generates `SELECT ... FOR UPDATE`. All stock mutations (`reserveStock`, `releaseReservation`, `confirmStock`) call this within `@Transactional`. |
| **Repository** | Extend `JpaRepository<Inventory, Long>`. Add pessimistic lock query method. Keep existing query methods (`findLowStockItems`, `findStaleReservations`). |
| **Service** | `@Service` with `@Transactional` on all mutation methods. Logic unchanged except locking. Remove any reactive types (`Mono`/`Flux`). |
| **Controller** | `@RestController` (MVC) returning `ResponseEntity<InventoryDto>` etc. Endpoints unchanged. |
| **Concurrency Test** | Critical: integration test simulating 100 concurrent `reserveStock` calls for same variant → verify total reserved ≤ quantity. |
| **Events** | RabbitMQ publishing unchanged. `OutboxEventPublisher` continues working. |

### Shared / Platform

| Decision | Detail |
|----------|--------|
| **Common DTOs** | No changes. `ProductDto`, `ProductVariantDto`, `InventoryDto` remain in `common` module. |
| **Gateway Routes** | Unchanged: `lb://product`, `lb://inventory-service`. |
| **Event Contracts** | `InventoryEvent`, `ProductEvent` in `common.event` unchanged. |
| **Docker Compose** | Add MongoDB service. Ensure PostgreSQL for inventory. |
| **Config Server** | Add MongoDB connection config for product service. |
| **Observability** | Micrometer metrics on MongoDB reactive driver; JPA query timings on inventory. |

## Testing Decisions

### What Makes a Good Test

- **Test external behavior only**: HTTP request → response, event published, database state changed.
- **No implementation detail assertions**: Don't verify internal method calls, repository calls, or locking mechanism — verify *outcome* (no oversell, correct stock numbers).
- **Use highest seam**: Gateway integration tests for full flow; service-level integration tests for repository + event publishing.

### Product Service (MongoDB)

| Test Type | Approach |
|-----------|----------|
| **Unit** | Mock `ReactiveMongoRepository`; verify service transforms correctly; test search/filter logic. |
| **Integration** | Testcontainers MongoDB; `WebTestClient` against running service; verify CRUD, search, pagination, text search. |
| **Contract** | Pact consumer tests from Gateway perspective (already defined). |
| **Data Migration** | Integration test: run migration script → verify document count, embedded variants, query results. |

### Inventory Service (Pessimistic Locking)

| Test Type | Approach |
|-----------|----------|
| **Unit** | Mock `JpaRepository`; verify service calls `findByVariantIdWithLock`; test reservation logic (full, partial, backorder). |
| **Integration** | Testcontainers PostgreSQL; `MockMvc` against running service; verify stock mutations, event publishing. |
| **Concurrency** | **Critical**: Spawn N threads calling `reserveStock` concurrently; assert `sum(reserved) <= quantity`. Use `CountDownLatch` for synchronization. |
| **Contract** | Pact consumer tests from Gateway perspective (already defined). |

### Prior Art in Codebase

- `OrderServiceTest.java`: Mockito unit tests with `RabbitTemplate` verification — pattern for service unit tests.
- `system-test` module: Testcontainers PostgreSQL + RabbitMQ — pattern for integration tests.
- `InventoryServiceTest.java`: Existing integration tests with Testcontainers — extend for concurrency.
- `ProductVariantMapper.java` / `ProductMapper.java`: MapStruct patterns — adapt for MongoDB documents.

## Out of Scope

- Elasticsearch integration (MongoDB text search sufficient for MVP; Elasticsearch can be added later).
- Product image/media management (S3/CDN).
- Inventory multi-location/warehouse support (single location per variant for now).
- Real-time stock WebSocket push to frontend.
- Advanced pricing rules (promotions, bundles) — handled in separate pricing service future.
- Redis for Gateway rate-limiting (planned separately).
- Category service changes (remains WebMVC + JPA).

## Further Notes

### Seams for Testing (Highest Possible)

1. **Gateway HTTP seam** — Contract tests via Pact (existing).
2. **Service HTTP seam** — `WebTestClient` / `MockMvc` against running service (existing pattern).
3. **RabbitMQ event seam** — `OutboxEventPublisher` integration tests verify events published with correct payload (existing pattern).
4. **Database seam** — Testcontainers PostgreSQL/MongoDB for repository integration tests (existing pattern).

**No new seams introduced.** All testing uses existing seams.

### Dependencies / Ordering

1. **Inventory first** (lower risk: stays on PostgreSQL, only locking change).
2. **Product second** (higher risk: data model + stack + migration).
3. **Gateway/Config/Eureka** — no changes required.
4. **System tests** — update `docker-compose.test.yml` for MongoDB; verify E2E flow passes.

### Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| Product data migration fails | Run migration in staging first; keep PostgreSQL read-only during cutover; rollback plan = revert deployment. |
| MongoDB reactive learning curve | Pair with team member experienced in Reactive MongoDB; start with simple CRUD, add search later. |
| Pessimistic locking deadlocks | Keep transactions short; no external calls inside locked section; test with high concurrency. |
| Event schema drift | Contract tests on `common` module DTOs + events; CI fails on breaking changes. |

### Timeline Estimate

| Phase | Effort |
|-------|--------|
| Inventory Service refactor | 2-3 days |
| Product Service MongoDB migration | 4-5 days |
| Migration script + validation | 1 day |
| Integration + concurrency tests | 2 days |
| E2E validation | 1 day |
| **Total** | **~10-12 days** |

### Related Tickets

- `domain-model/03a-product-variant` → maps to Product service MongoDB migration
- `domain-model/05a-inventory-core` → maps to Inventory pessimistic locking
- `full-build/05-product-service` → platform ticket for Product
- `full-build/07-inventory-service` → platform ticket for Inventory

---

**Triage Label**: `ready-for-agent`
