# 05 — Product Service

**What to build:** Product catalog service at `http://localhost:8081`. WebFlux + JPA + PostgreSQL. REST + OpenAPI.

**Blocked by:** 01-config-server, 02-eureka-server, 04-gateway

**Status:** ready-for-agent

- [ ] Service starts on port 8081, registers with Eureka
- [ ] PostgreSQL schema: `product`, `product_variant`, `product_attribute`
- [ ] Endpoints: `GET /api/v1/products?cursor=&limit=`, `GET /api/v1/products/{id}`, `POST /api/v1/products`
- [ ] Search/filter by category, price range, attributes
- [ ] Cursor-based pagination
- [ ] OpenAPI docs at `/swagger-ui.html`
- [ ] Actuator health + Prometheus metrics
- [ ] Unit tests (domain logic) + Integration tests (Testcontainers PG)
- [ ] Dockerfile + docker-compose entry