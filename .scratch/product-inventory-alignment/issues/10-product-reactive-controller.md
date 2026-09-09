# 10 — Product: Reactive Controller

**What to build:** `ProductController` as WebFlux `@RestController` returning `Flux<ProductDto>` / `Mono<ProductDto>`. Endpoints: `GET /api/products`, `GET /api/products/{id}`, `GET /api/products/search`, `GET /api/products/filter`, `POST /api/products`, `PUT /api/products/{id}`, `DELETE /api/products/{id}`. Pagination via `Pageable`.

**Blocked by:** 09 — Product: Reactive Service Layer

**Status:** ready-for-agent

- [ ] Change controller to reactive: return `Flux<ProductDto>`, `Mono<ResponseEntity<ProductDto>>`, `Mono<Void>`
- [ ] `GET /api/products` → `Flux<ProductDto>` (with optional `categoryId`, `page`, `size` params)
- [ ] `GET /api/products/{id}` → `Mono<ResponseEntity<ProductDto>>`
- [ ] `GET /api/products/search?q={query}` → `Flux<ProductDto>`
- [ ] `GET /api/products/filter` with query params for attributes → `Flux<ProductDto>`
- [ ] `POST /api/products` → `Mono<ResponseEntity<ProductDto>>` (201 on success)
- [ ] `PUT /api/products/{id}` → `Mono<ResponseEntity<ProductDto>>`
- [ ] `DELETE /api/products/{id}` → `Mono<ResponseEntity<Void>>`
- [ ] Keep same `ProductDto` from `common` module
- [ ] Verify `mvn compile -pl product` succeeds
- [ ] Integration test (Testcontainers MongoDB): `WebTestClient` calls all endpoints; verify CRUD, search, filter, pagination