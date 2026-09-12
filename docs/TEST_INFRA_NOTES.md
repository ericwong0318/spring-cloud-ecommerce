# Test Infrastructure Notes

## Problem: "Two Databases" Conflict
- Flyway (JDBC) → Testcontainers DB (dynamic port) ✅ Tables created
- R2DBC (App) → localhost:5432 (hardcoded) ❌ Empty DB → "relation 'payments' does not exist"

## Root Cause
Flyway runs during Spring context initialization (before `@DynamicPropertySource` applies). R2DBC reads hardcoded `localhost:5432` from `application.yml`.

## Solution
1. Use `@DynamicPropertySource` to dynamically set both JDBC and R2DBC URLs to the same Testcontainers PostgreSQL instance
2. Disable Flyway auto-config: `spring.flyway.enabled: false` in `application-test.yml`
3. Run Flyway manually via `ContextRefreshedEvent` listener after DataSource is created

## Key Files
- `payment-service/pom.xml` - added `spring-boot-testcontainers`, `flyway-database-postgresql:11.7.0`, `HikariCP`
- `BaseIntegrationTest.java` - test infrastructure
- `application-test.yml` - test profile config
