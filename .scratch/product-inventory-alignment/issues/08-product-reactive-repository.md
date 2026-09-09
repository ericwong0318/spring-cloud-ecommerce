# 08 — Product: Reactive Repository with Query Methods

**What to build:** `ProductRepository` extends `ReactiveMongoRepository<Product, String>` with custom query methods: find by category, text search, attribute filters, pagination.

**Blocked by:** 07 — Product: MongoDB Document Model

**Status:** ready-for-agent

- [ ] Create `ProductRepository extends ReactiveMongoRepository<Product, String>`
- [ ] Add `Flux<Product> findByCategoryId(Long categoryId)`
- [ ] Add `Flux<Product> findByNameContainingIgnoreCase(String name)` (text search)
- [ ] Add `Flux<Product> findByVariantsAttributesKeyAndVariantsAttributesValue(String key, String value)`
- [ ] Add pagination support: `findByCategoryId(Long categoryId, Pageable pageable)`
- [ ] Verify `mvn compile -pl product` succeeds
- [ ] Unit test: mock repository, verify query methods called with correct params