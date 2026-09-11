# 10 — Product: Reactive Controller

**What to build:** `ProductController` as WebFlux `@RestController` returning `Flux<ProductDto>` / `Mono<ProductDto>`. Endpoints: `GET /api/products`, `GET /api/products/{id}`, `GET /api/products/search`, `GET /api/products/filter`, `POST /api/products`, `PUT /api/products/{id}`, `DELETE /api/products/{id}`. Pagination via `Pageable`.

**Blocked by:** 09 — Product: Reactive Service Layer

**Status:** ready-for-agent

- [x] Change controller to reactive: return `Flux<ProductDto>`, `Mono<ResponseEntity<ProductDto>>`, `Mono<Void>`
- [x] `GET /api/products` → `Flux<ProductDto>` (with optional `categoryId`, `page`, `size` params)
- [x] `GET /api/products/{id}` → `Mono<ResponseEntity<ProductDto>>`
- [x] `GET /api/products/search?q={query}` → `Flux<ProductDto>`
- [x] `GET /api/products/filter` with query params for attributes → `Flux<ProductDto>`
- [x] `POST /api/products` → `Mono<ResponseEntity<ProductDto>>` (201 on success)
- [x] `PUT /api/products/{id}` → `Mono<ResponseEntity<ProductDto>>`
- [x] `DELETE /api/products/{id}` → `Mono<ResponseEntity<Void>>`
- [x] Keep same `ProductDto` from `common` module
- [ ] Verify `mvn compile -pl product` succeeds (blocked by Java 21 not available)
- [ ] Integration test (Testcontainers MongoDB): `WebTestClient` calls all endpoints; verify CRUD, search, filter, pagination
