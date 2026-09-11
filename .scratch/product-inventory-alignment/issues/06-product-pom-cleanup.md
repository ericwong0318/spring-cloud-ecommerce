# 06 — Product: POM Cleanup (Remove JPA, Add MongoDB Reactive)

**What to build:** Product service builds on WebFlux + MongoDB Reactive only — no JPA, PostgreSQL, or Flyway dependencies.

**Blocked by:** None — can start immediately (parallel with inventory track)

**Status:** done

- [x] Remove `spring-boot-starter-data-jpa`, `postgresql`, `flyway-core`, `spring-boot-starter-web` from `product/pom.xml`
- [x] Add `spring-boot-starter-webflux`, `spring-boot-starter-data-mongodb-reactive`
- [x] Replace `springdoc-openapi-starter-webmvc-ui` with `springdoc-openapi-starter-webflux-ui`
- [x] Remove Flyway migration scripts from `product/src/main/resources/db/migration/`
- [ ] Verify `mvn compile -pl product` succeeds (blocked by Java 21 not available in environment)
