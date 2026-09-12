# 06 — Fix Mixed Reactive/Blocking in notification-service

**What to build:** Resolve the blocking JPA repository usage inside reactive WebFlux controllers in notification-service. Either migrate to R2DBC (reactive) or switch to Spring MVC (blocking) consistently.

**Blocked by:** None — can start immediately.

**Status:** ready-for-agent

- [x] Decision: Choose one — (A) Migrate to R2DBC + Reactive repositories, or (B) Switch to Spring MVC + JPA → **(B) Switch to Spring MVC + JPA**
- [ ] If (A): Replace `spring-boot-starter-data-jpa` with `spring-boot-starter-data-r2dbc` + `r2dbc-postgresql` in notification-service/pom.xml
- [ ] If (A): Convert `NotificationRepository` and `NotificationTemplateRepository` to `R2dbcRepository` with reactive return types (`Mono`, `Flux`)
- [ ] If (A): Update `NotificationService` to use reactive repository methods
- [ ] If (A): Update `NotificationController` to return `Mono`/`Flux` (already does)
- [x] If (B): Replace `spring-boot-starter-webflux` with `spring-boot-starter-web` in pom.xml
- [x] If (B): Change `NotificationController` from reactive to blocking (remove `Mono`/`Flux`, use `ResponseEntity` directly)
- [x] If (B): Update `NotificationService` to use blocking calls
- [x] Remove lombok from notification-service (see ticket 01) — done in prior commit 4991a29
- [x] Run tests: `mvn test -pl notification-service` and `mvn verify -pl notification-service` — all 8 tests pass, BUILD SUCCESS
- [x] Additional: Replace `@RabbitListener` reactive pattern with blocking RestClient RPC to order-service; add JwtDecoderConfig for OAuth2 resource server; make RabbitMQ optional via spring.autoconfigure.exclude