# ADR-002: Database Strategy — PostgreSQL Per Service

## Status
Accepted

## Context
Each service needs its own data store. Need to choose database technology and ownership model.

## Decision
**PostgreSQL per service** — each service owns its schema and data. No shared databases.

| Service | Database | Access Pattern |
|---------|----------|----------------|
| product | PostgreSQL | Spring Data JPA (blocking) |
| category | PostgreSQL | Spring Data JPA (blocking) |
| inventory | PostgreSQL | Spring Data R2DBC (reactive) |
| order | PostgreSQL | Spring Data R2DBC (reactive) |
| payment | PostgreSQL | Spring Data JPA (blocking) |
| auth-server | PostgreSQL | Spring Data JPA (blocking) |

Config server and Eureka use in-memory/embedded stores. Gateway is stateless.

## Rationale
- **Single technology**: PostgreSQL handles both JPA and R2DBC, reducing operational burden
- **ACID per service**: Strong consistency within service boundaries
- **Testcontainers**: First-class PostgreSQL support for integration tests
- **Production parity**: Same DB locally, in CI, and in production
- **Team autonomy**: Each service team chooses schema evolution strategy

## Consequences
- **Positive**: Consistent tooling, familiar SQL, mature ecosystem, supports both reactive and blocking
- **Negative**: Multiple PostgreSQL instances (resource usage), schema duplication risk
- **Mitigation**: Docker Compose with shared PostgreSQL container (multiple databases), Flyway per service

## Alternatives Considered
- **H2 for reactive services**: R2DBC H2 support is limited; not production-representative
- **MongoDB for product/catalog**: Adds second technology; PostgreSQL JSONB sufficient
- **Shared database**: Violates service autonomy, creates coupling