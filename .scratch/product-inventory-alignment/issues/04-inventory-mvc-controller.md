# 04 — Inventory: MVC Controller

**What to build:** `InventoryController` as Spring MVC `@RestController` (not WebFlux). Endpoints unchanged: `POST /api/inventory/reserve`, `DELETE /api/inventory/reserve/{id}`, `POST /api/inventory/confirm`, `GET /api/inventory/{variantId}`. Returns `ResponseEntity<InventoryDto>`.

**Blocked by:** 03 — Inventory: Service Mutations Under Pessimistic Lock

**Status:** done

- [x] Change controller from `@RestController` (WebFlux) to Spring MVC `@RestController`
- [x] Return `ResponseEntity<InventoryDto>` / `ResponseEntity<Void>` instead of `Mono<ResponseEntity<...>>`
- [x] Keep same request/response DTOs from `common` module
- [x] Verify `mvn compile -pl inventory-service` succeeds
- [x] Integration test (Testcontainers PostgreSQL): `MockMvc` calls reserve → confirm → release; verify DB state + RabbitMQ events