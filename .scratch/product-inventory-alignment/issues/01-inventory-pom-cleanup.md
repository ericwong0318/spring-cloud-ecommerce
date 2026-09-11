# 01 — Inventory: POM Cleanup (Remove WebFlux/R2DBC, Add WebMVC)

**What to build:** Inventory service builds and starts on Spring MVC + JPA only — no WebFlux or R2DBC dependencies. All reactive imports removed from project.

**Blocked by:** None — can start immediately.

**Status:** done

- [x] Remove `spring-boot-starter-webflux`, `spring-boot-starter-data-r2dbc`, `r2dbc-postgresql` from `inventory-service/pom.xml` (already not present)
- [x] Add `spring-boot-starter-web`, `springdoc-openapi-starter-webmvc-ui` (already present)
- [x] Remove `reactor-test` from test scope in `inventory-service/pom.xml`
- [x] Remove R2DBC autoconfigure exclusions from test config (`application-test.yml` and `ReservationExpirySchedulerIntegrationTest.java`)
- [x] Verify `mvn compile -pl inventory-service` succeeds
- [x] Verify unit tests pass (`mvn test -pl inventory-service -Dtest=InventoryServiceTest`)