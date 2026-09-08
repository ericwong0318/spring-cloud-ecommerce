# ADR-004: API Style — REST + OpenAPI 3

## Status
Accepted

## Context
Need a consistent API style for service-to-service and external communication.

## Decision
**RESTful HTTP/JSON with OpenAPI 3.0** for all APIs.

### Standards
- **Resource-oriented URLs**: `GET /api/v1/products/{id}`, `POST /api/v1/orders`
- **HTTP verbs**: GET, POST, PUT, PATCH, DELETE per RFC 7231
- **Status codes**: 200, 201, 204, 400, 401, 403, 404, 409, 422, 500
- **Error format**: RFC 7807 Problem Details (`application/problem+json`)
- **Pagination**: Cursor-based (`?cursor=&limit=20`) for collections
- **Versioning**: URL path (`/api/v1/`) — breaking changes = new version
- **Documentation**: OpenAPI 3.0 annotations → `/v3/api-docs` + Swagger UI at `/swagger-ui.html`

### Service-to-Service
- Gateway routes via `lb://service-name` (Eureka)
- Services communicate via `WebClient` (reactive) or `RestClient` (blocking)
- Timeouts: 5s connect, 10s read
- Retries: 3x with exponential backoff for idempotent operations

## Rationale
- **Ubiquitous**: REST is understood by all team members and consumers
- **Tooling**: SpringDoc/OpenAPI generates specs, clients, tests automatically
- **Gateway-friendly**: Spring Cloud Gateway natively routes REST
- **Observability**: HTTP semantics map to metrics/logs naturally
- **Interoperability**: External consumers (mobile, web, partners) expect REST

## Consequences
- **Positive**: Broad compatibility, excellent tooling, cacheable, debuggable
- **Negative**: Over-fetching/under-fetching, no built-in schema evolution
- **Mitigation**: Sparse fieldsets (`?fields=id,name,price`), versioned APIs

## Alternatives Considered
- **GraphQL gateway**: Adds complexity (N+1, schema stitching) without clear benefit for 9 services
- **gRPC internal**: Requires protobuf tooling, harder to debug, gateway translation layer needed
- **Async-only (event-driven)**: Query patterns need request-response; CQRS adds complexity