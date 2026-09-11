# ADR-007: Test Strategy — Full Pyramid

## Status
Accepted

## Context
Production-ready services need comprehensive test coverage across all layers.

## Decision
**Full test pyramid** with four layers:

```
                    ┌─────────────┐
                    │   E2E (1%)  │  ← Playwright against docker-compose
                    ├─────────────┤
                    │ Contract (5%)│  ← Pact (consumer-driven) between gateway↔services
                    ├─────────────┤
              ┌─────┤Integration(20%)├─────┐  ← Testcontainers (PostgreSQL, RabbitMQ, services)
              │     └─────────────┘     │
              │                         │
      ┌───────▼───────┐         ┌───────▼───────┐
      │  Unit (74%)   │         │  Unit (74%)   │
      │  (domain,     │         │  (controllers,│
      │   services,   │         │   mappers,    │
      │   mappers)    │         │   config)     │
      └───────────────┘         └───────────────┘
```

### Layer Details

| Layer | Tool | Scope | Target |
|-------|------|-------|--------|
| **Unit** | JUnit 5 + Mockito | Pure logic, no Spring context | >80% line coverage |
| **Integration** | Spring Boot Test + Testcontainers | Repository, WebClient, saga orchestration, RabbitMQ | All happy/error paths |
| **Contract** | Pact JVM | Gateway ↔ Service APIs | Consumer-driven, published to broker |
| **E2E** | Playwright | Full user journeys (browse→order→pay) | Critical paths only |

### Testcontainers Configuration
- Shared PostgreSQL container per test class (not per test)
- Shared RabbitMQ container per test class
- `@DynamicPropertySource` for datasource URL + RabbitMQ connection
- `spring.sql.init.mode=always` for Flyway migration
- Parallel execution: `forkCount=2`, `reuseForks=true`

### CI Pipeline
```yaml
# .github/workflows/ci.yml
jobs:
  test:
    steps:
      - mvn test -pl <module>           # Unit
      - mvn verify -pl <module>         # Integration (Testcontainers: PostgreSQL + RabbitMQ)
      - mvn pact:verify -pl gateway     # Contract
  e2e:
    - docker-compose -f docker-compose.test.yml up -d
    - npx playwright test               # E2E
```

## Rationale
- **Confidence**: Each layer catches different failure modes
- **Speed**: Unit tests run in seconds; integration in minutes
- **Contract tests**: Prevent breaking changes between gateway and services
- **Testcontainers**: Real PostgreSQL + RabbitMQ = no H2/embedded surprises
- **E2E**: Validates deployment topology, not just code

## Consequences
- **Positive**: High confidence, catches integration bugs, documents APIs
- **Negative**: CI time (10-15 min), Testcontainers resource usage, Pact maintenance
- **Mitigation**: Parallel test execution, shared containers, contract tests only for gateway↔domain boundaries

## Alternatives Considered
- **Unit + integration only**: Misses API contract breaks
- **Heavy E2E, light unit**: Slow feedback, flaky, hard to debug
- **No contract tests**: Version skew between gateway and services
