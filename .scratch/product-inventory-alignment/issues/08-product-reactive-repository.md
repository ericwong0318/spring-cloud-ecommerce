# 08 — Product: Reactive Repository with Query Methods

**What to build:** `ProductRepository` extends `ReactiveMongoRepository<Product, String>` with custom query methods: find by category, text search, attribute filters, pagination.

**Blocked by:** 07 — Product: MongoDB Document Model

**Status:** ready-for-agent

- [x] Create `ProductRepository extends ReactiveMongoRepository<Product, String>`
- [x] Add `Flux<Product> findByCategoryId(String categoryId)`
- [x] Add `Flux<Product> findByNameContainingIgnoreCase(String name)` (text search)
- [x] Add `Flux<Product> findByVariantsAttributesKeyAndVariantsAttributesValue(String key, String value)`
- [x] Add pagination support: `findByCategoryId(String categoryId, Pageable pageable)`
- [ ] Verify `mvn compile -pl product` succeeds (blocked by Java 21 not available)
- [ ] Unit test: mock repository, verify query methods called with correct params
