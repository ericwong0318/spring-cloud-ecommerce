# 09 — Product: Reactive Service Layer

**What to build:** `ProductService` with reactive methods (`Mono<ProductDto>`, `Flux<ProductDto>`) for CRUD, search, filter. No `@Transactional` (MongoDB transactions not needed for single-document ops). Maps document ↔ DTO.

**Blocked by:** 08 — Product: Reactive Repository

**Status:** ready-for-agent

- [x] `getAllProducts()` → `Flux<ProductDto>` via `repository.findAll().map(mapper::toDto)`
- [x] `getProductById(String id)` → `Mono<ProductDto>` via `repository.findById(id).map(mapper::toDto)`
- [x] `getProductsByCategory(String categoryId, Pageable pageable)` → `Flux<ProductDto>`
- [x] `searchProducts(String query, Pageable pageable)` → `Flux<ProductDto>` (text search)
- [x] `filterByAttributes(Map<String,String> attrs, Pageable pageable)` → `Flux<ProductDto>`
- [x] `createProduct(ProductDto dto)` → `Mono<ProductDto>` (save new document)
- [x] `updateProduct(String id, ProductDto dto)` → `Mono<ProductDto>` (find + update + save)
- [x] `deleteProduct(String id)` → `Mono<Void>`
- [x] Remove `@Transactional` annotations
- [ ] Verify `mvn compile -pl product` succeeds (blocked by Java 21 not available)
- [ ] Unit tests: mock `ReactiveMongoRepository`, verify reactive chain transformations