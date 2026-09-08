# E-Commerce Microservices Platform — Project Context

## Vision
A production-ready, portfolio-quality e-commerce platform demonstrating Spring Cloud microservices patterns. Built for learning and showcase.

## Scope (9 Services)

| Service | Purpose | Tech |
|---------|---------|------|
| **config-server** | Centralized configuration (Git backend) | Spring Cloud Config |
| **eureka-server** | Service discovery & registration | Netflix Eureka |
| **gateway** | API Gateway, routing, OAuth2 resource server | Spring Cloud Gateway |
| **auth-server** | OAuth2/OIDC authorization server, JWT issuance | Spring Authorization Server |
| **product** | Product catalog, search, categories | WebFlux + JPA + PostgreSQL |
| **category** | Category hierarchy management | WebMVC + JPA + PostgreSQL |
| **inventory** | Stock levels, reservations, allocation | WebFlux + R2DBC + PostgreSQL |
| **order** | Order lifecycle, saga orchestration | WebFlux + R2DBC + PostgreSQL |
| **payment** | Payment processing (mocked gateway) | WebFlux + JPA + PostgreSQL |

## Out of Scope
- Notification service (email/SMS)
- User profile/address management (delegated to auth-server)
- Shopping cart (merged into order service)
- Admin dashboard / UI
- Contract testing with Pact (deferred)
- Multi-tenancy

## Key Architectural Decisions

1. **Service decomposition**: 9 services (3 infra + 6 domain) matching the template
2. **Database**: PostgreSQL per service (no shared databases)
3. **Consistency**: Eventual consistency via sagas + RabbitMQ
4. **API**: REST + OpenAPI 3 for all service-to-service and external communication
5. **Auth**: Gateway validates JWT, downstream services trust gateway headers
6. **Events**: RabbitMQ (topic exchange, Spring Cloud Stream)
7. **Tests**: Full pyramid — unit, contract, integration (Testcontainers), e2e
8. **Deploy**: Docker Compose locally; Kubernetes-ready artifacts
9. **Timeline**: 2-3 weeks to running skeleton

## Domain Boundaries

```
┌─────────────────────────────────────────────────────────────┐
│                        GATEWAY (8080)                        │
│              Routes → lb://service-name                      │
└─────────────────────────────────────────────────────────────┘
         │              │              │              │
         ▼              ▼              ▼              ▼
   ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐
   │ PRODUCT  │  │ CATEGORY │  │ INVENTORY│  │  ORDER   │
   │  (8081)  │  │  (8082)  │  │  (8084)  │  │  (8083)  │
   └──────────┘  └──────────┘  └──────────┘  └──────────┘
         │                                │              │
         └────────────────────────────────┼──────────────┘
                                          ▼
                                  ┌──────────────┐
                                  │  PAYMENT     │
                                  │   (8085)     │
                                  └──────────────┘
```

## Success Criteria
- [ ] All 9 services start via `docker-compose up`
- [ ] End-to-end order flow: browse → add to cart → checkout → payment → order confirmed
- [ ] Inventory reserved on order, released on payment failure
- [ ] OpenAPI docs at `/swagger-ui.html` on each service
- [ ] Actuator health/prometheus on all services
- [ ] Unit + integration tests passing (`mvn verify`)
- [ ] GitHub Actions CI green