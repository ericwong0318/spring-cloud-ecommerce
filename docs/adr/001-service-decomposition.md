# ADR-001: Service Decomposition — 9 Services

## Status
Accepted

## Context
Need to define service boundaries for an e-commerce platform that demonstrates Spring Cloud microservices patterns while remaining portfolio-ready and achievable in 2-3 weeks.

## Decision
Decompose into **9 services**:

| Category | Service | Responsibility |
|----------|---------|----------------|
| **Infrastructure** | config-server | Centralized configuration from Git |
| | eureka-server | Service discovery & registration |
| | gateway | API Gateway, routing, OAuth2 resource server |
| | auth-server | OAuth2/OIDC authorization server, JWT issuance |
| **Domain** | product | Product catalog, search, attributes |
| | category | Category hierarchy, navigation |
| | inventory | Stock levels, reservations, allocation |
| | order | Order lifecycle, saga orchestration |
| | payment | Payment processing (mocked gateway) |

## Rationale
- Matches the existing Spring Cloud project template (11 services minus notification, user-profile, cart)
- Each service owns a single business capability
- Infrastructure services are reusable patterns for any microservices platform
- Domain services cover core e-commerce flow: browse → select → reserve → pay → confirm
- 9 services is the minimum to demonstrate: service discovery, config, gateway, auth, inter-service comm, sagas, reactive + blocking stacks

## Consequences
- **Positive**: Clear boundaries, independent deployability, technology heterogeneity (WebFlux + WebMVC), portfolio breadth
- **Negative**: Operational complexity (9 services to run), network latency, distributed debugging
- **Mitigation**: Docker Compose for local dev, shared testcontainers config, structured logging with correlation IDs

## Alternatives Considered
- **6 services** (gateway + config + eureka + 3 domains): Too few to demonstrate auth-server, separate inventory/payment patterns
- **11 services** (full template): Notification and cart add complexity without new patterns
- **Monolith**: Defeats the learning goal