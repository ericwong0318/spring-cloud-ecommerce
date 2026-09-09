# 02 — Eureka Server

**What to build:** Netflix Eureka Server for service discovery at `http://localhost:8761`. Services register via `spring.cloud.service-registry.auto-registration.enabled=true`.

**Blocked by:** None — can start immediately.

**Status:** ready-for-agent

- [ ] Eureka Server starts on port 8761
- [ ] Dashboard shows registered services
- [ ] Self-preservation mode configured for dev
- [ ] Actuator health endpoint
- [ ] OpenAPI docs at `/swagger-ui.html`
- [ ] Dockerfile + docker-compose entry