# ADR-002: Database Strategy — PostgreSQL Per Service (Product uses PostgreSQL JSONB)

## Status
Accepted (Amended)

## Context
Each service needs its own data store. Need to choose database technology and ownership model.

## Decision
**PostgreSQL per service** — each service owns its schema and data. No shared databases.
**Product service**: Uses PostgreSQL with R2DBC and JSONB for flexible product catalog attributes.

| Service | Database | Access Pattern |
|---------|----------|----------------|
| product | PostgreSQL | Spring Data R2DBC (reactive) + JSONB |
| category | PostgreSQL | Spring Data JPA (blocking) |
| inventory | PostgreSQL | Spring Data JPA (blocking) |
| order | PostgreSQL | Spring Data R2DBC (reactive) |
| payment | PostgreSQL | Spring Data R2DBC (reactive) |
| auth-server | PostgreSQL | Spring Data JPA (blocking) |
| notification | PostgreSQL | Spring Data JPA (blocking) |

Config server and Eureka use in-memory/embedded stores. Gateway is stateless.

## Rationale
- **Single technology**: All services use PostgreSQL, reducing operational burden to one database technology
- **Product service**: Uses PostgreSQL R2DBC + JSONB for flexible product catalog with variable attributes (size, color, storage, etc.). JSONB provides MongoDB-like flexibility with ACID transactions
- **ACID per service**: Strong consistency within service boundaries
- **Testcontainers**: First-class PostgreSQL support for integration tests (single container type)
- **Production parity**: Same DB locally, in CI, and in production (Neon PostgreSQL)
- **Team autonomy**: Each service team chooses schema evolution strategy via Flyway migrations
- **Neon integration**: Production uses Neon PostgreSQL with PgBouncer pooled endpoint for connection management

## Consequences
- **Positive**: Single database technology, consistent tooling, familiar SQL, mature ecosystem, supports both reactive (R2DBC) and blocking (JPA) access patterns, JSONB provides document-like flexibility
- **Negative**: JSONB queries are less ergonomic than MongoDB's native document queries; requires careful indexing strategy (GIN indexes for attribute lookups)
- **Mitigation**: GIN indexes on JSONB attributes column, Flyway per service for schema evolution, Neon PgBouncer for connection pooling

## Alternatives Considered
- **H2 for reactive services**: R2DBC H2 support is limited; not production-representative
- **MongoDB for all services**: Adds second technology broadly; PostgreSQL JSONB sufficient for most use cases including product catalog
- **Shared database**: Violates service autonomy, creates coupling
