# ADR 008: Remove H2, Use PostgreSQL Everywhere

## Status
Accepted

## Context
The project currently uses H2 in-memory database for local development (`dev` profile) and PostgreSQL for Docker/production. Several services use H2 in tests (`category`, `auth-server` via config-server defaults).

Problems with H2:
- Not production-representative (different SQL dialect, types, behavior)
- Causes subtle bugs that only surface in CI/production
- Spring Authorization Server OAuth2 tables have PostgreSQL-specific migrations
- Testcontainers PostgreSQL is already the standard for integration tests in most services
- Maintaining dual dialect configurations adds complexity

## Decision
**Remove H2 entirely. Use PostgreSQL everywhere via Testcontainers for tests and Docker for local development.**

Changes:
1. Remove `com.h2database:h2` from root `pom.xml` dependencyManagement and all module `pom.xml` files
2. Update config-server configurations: rename `docker` profile to `dev`, make it default (PostgreSQL)
3. Update all test configurations to use Testcontainers PostgreSQL (standardize on pattern used by `inventory-service`, `order-service`, `payment-service`)
4. Update `auth-server` to use PostgreSQL via Testcontainers for tests
5. Document Docker/OrbStack as prerequisite for local development

## Consequences
### Positive
- Single database dialect everywhere (PostgreSQL)
- Tests run against real PostgreSQL — catches dialect/type issues early
- Simplified configuration (no dual-profile maintenance)
- Aligns with existing Testcontainers investment

### Negative
- **Requires Docker/OrbStack running for all local development and tests**
- Slightly slower test startup (Testcontainers container initialization)
- Developers without Docker cannot run tests locally

### Migration Notes
- Existing `docker` profiles in config-server renamed to `dev` and activated by default
- Flyway migrations already PostgreSQL-compatible
- Testcontainers PostgreSQL module already in root BOM

## References
- ADR 002: Database Strategy (noted H2 limitations for reactive services)
- ADR 007: Test Strategy (mandates Testcontainers)