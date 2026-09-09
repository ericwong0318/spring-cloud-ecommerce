# 09 — Product: Reactive Service Layer

**What to build:** `ProductService` with reactive methods (`Mono<ProductDto>`, `Flux<ProductDto>`) for CRUD, search, filter. No `@Transactional` (MongoDB transactions not needed for single-document ops). Maps document ↔ DTO.

**Blocked by:** 08 — Product: Reactive Repository

**Status:** ready-for-agent

- [ ] `getAllProducts()` → `Flux<ProductDto>` via `repository.findAll().map(mapper::toDto)`
- [ ] `getProductById(String id)` → `Mono<ProductDto>` via `repository.findById(id).map(mapper::toDto)`
- [ ] `getProductsByCategory(Long categoryId, Pageable pageable)` → `Flux<ProductDto>`
- [ ] `searchProducts(String query, Pageable pageable)` → `Flux<ProductDto>` (text search)
- [ ] `filterByAttributes(Map<String,String> attrs, Pageable pageable)` → `Flux<ProductDto>`
- [ ] `createProduct(ProductDto dto)` → `Mono<ProductDto>` (save new document)
- [ ] `updateProduct(String id, ProductDto dto)` → `Mono<ProductDto>` (find + update + save)
- [ ] `deleteProduct(String id)` → `Mono<Void>`
- [ ] Remove `@Transactional` annotations
- [ ] Verify `mvn compile -pl product` succeeds
- [ ] Unit tests: mock `ReactiveMongoRepository`, verify reactive chain transformations