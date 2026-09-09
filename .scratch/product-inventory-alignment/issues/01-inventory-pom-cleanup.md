# 01 — Inventory: POM Cleanup (Remove WebFlux/R2DBC, Add WebMVC)

**What to build:** Inventory service builds and starts on Spring MVC + JPA only — no WebFlux or R2DBC dependencies. All reactive imports removed from project.

**Blocked by:** None — can start immediately.

**Status:** ready-for-agent

- [ ] Remove `spring-boot-starter-webflux`, `spring-boot-starter-data-r2dbc`, `r2dbc-postgresql` from `inventory-service/pom.xml`
- [ ] Add `spring-boot-starter-web`, `springdoc-openapi-starter-webmvc-ui`
- [ ] Remove `spring-boot-starter-webflux` from test scope
- [ ] Verify `mvn compile -pl inventory-service` succeeds
- [ ] Verify service starts with `mvn spring-boot:run -pl inventory-service` (no reactive beans)