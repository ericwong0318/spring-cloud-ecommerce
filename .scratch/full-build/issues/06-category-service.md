# 06 — Category Service

**What to build:** Category hierarchy service at `http://localhost:8082`. WebMVC + JPA + PostgreSQL. REST + OpenAPI.

**Blocked by:** 01-config-server, 02-eureka-server, 04-gateway

**Status:** ready-for-agent

- [ ] Service starts on port 8082, registers with Eureka
- [ ] PostgreSQL schema: `category` (self-referencing parent_id)
- [ ] Endpoints: `GET /api/v1/categories`, `GET /api/v1/categories/{id}`, `GET /api/v1/categories/tree`, `POST /api/v1/categories`
- [ ] Hierarchy traversal (ancestors, descendants, siblings)
- [ ] OpenAPI docs at `/swagger-ui.html`
- [ ] Actuator health + Prometheus metrics
- [ ] Unit tests + Integration tests (Testcontainers PG)
- [ ] Dockerfile + docker-compose entry