# 04 — Inventory: MVC Controller

**What to build:** `InventoryController` as Spring MVC `@RestController` (not WebFlux). Endpoints unchanged: `POST /api/inventory/reserve`, `DELETE /api/inventory/reserve/{id}`, `POST /api/inventory/confirm`, `GET /api/inventory/{variantId}`. Returns `ResponseEntity<InventoryDto>`.

**Blocked by:** 03 — Inventory: Service Mutations Under Pessimistic Lock

**Status:** ready-for-agent

- [ ] Change controller from `@RestController` (WebFlux) to Spring MVC `@RestController`
- [ ] Return `ResponseEntity<InventoryDto>` / `ResponseEntity<Void>` instead of `Mono<ResponseEntity<...>>`
- [ ] Keep same request/response DTOs from `common` module
- [ ] Verify `mvn compile -pl inventory-service` succeeds
- [ ] Integration test (Testcontainers PostgreSQL): `MockMvc` calls reserve → confirm → release; verify DB state + RabbitMQ events