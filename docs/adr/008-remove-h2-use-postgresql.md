# ADR 008: Use PostgreSQL for Docker/Prod, H2 for Dev Profile

## Status
Accepted (Amended)

## Context
The project originally planned to remove H2 entirely (ADR 008). However, the current implementation uses H2 for the default `dev` profile and PostgreSQL for `docker`/`prod` profiles.

Problems with H2:
- Not production-representative (different SQL dialect, types, behavior)
- Causes subtle bugs that only surface in CI/production
- Spring Authorization Server OAuth2 tables have PostgreSQL-specific migrations
- Testcontainers PostgreSQL is already the standard for integration tests in most services

Current reality:
- Default profile (`dev`) uses H2 in-memory databases for fast local development
- `docker` profile uses PostgreSQL via Docker Compose
- Integration tests use Testcontainers PostgreSQL
- Product service uses MongoDB (not PostgreSQL)

## Decision
**Keep H2 for `dev` profile for fast local development. Use PostgreSQL for `docker`/`prod` profiles and all integration tests via Testcontainers.**

Changes:
1. Rename `docker` profile to `dev` in config-server was not done - `docker` profile still exists
2. Default profile remains `dev` with H2
3. Integration tests use Testcontainers PostgreSQL (standardized on pattern used by `inventory-service`, `order-service`, `payment-service`)
4. Document Docker/OrbStack as prerequisite for integration tests and Docker deployment

## Consequences
### Positive
- Fast local development with H2 (no Docker required for basic dev)
- Tests run against real PostgreSQL — catches dialect/type issues early
- Simplified configuration (dual-profile maintenance is explicit)
- Aligns with existing Testcontainers investment

### Negative
- Dual dialect configurations to maintain
- Developers without Docker can run unit tests but not integration tests

### Migration Notes
- Existing `docker` profiles in config-server remain for Docker Compose deployment
- Flyway migrations already PostgreSQL-compatible
- Testcontainers PostgreSQL module already in root BOM

## References
- ADR 002: Database Strategy (noted H2 limitations for reactive services)
- ADR 007: Test Strategy (mandates Testcontainers)