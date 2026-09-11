# ADR-005: Auth Architecture — Gateway Validates, Services Trust

## Status
Accepted

## Context
Need authentication and authorization across 9 services with minimal coupling.

## Decision
**Gateway validates JWT; downstream services trust gateway headers.**

### Flow
```
1. Client → POST /oauth2/token (auth-server) → JWT access_token
2. Client → GET /api/... + Authorization: Bearer <token> → Gateway
3. Gateway → Validates JWT (signature, expiry, issuer, audience)
   ├─► Invalid → 401
   └─► Valid → Extract claims → Forward to service via headers:
       X-User-Id: <sub>
       X-User-Roles: <roles>
       X-Client-Id: <azp>
4. Service → Reads headers (no JWT validation)
   ├─► @PreAuthorize("hasRole('ADMIN')") works via header-based Authentication
   └─► Audit: userId from X-User-Id
```

### Implementation
- **Gateway**: `spring-boot-starter-oauth2-resource-server` + `JwtDecoder` from auth-server JWKS endpoint
- **Services**: Custom `AuthenticationManager` reading `X-User-*` headers; `SecurityContext` populated per request
- **Auth-server**: Spring Authorization Server; JWKS at `/oauth2/jwks`; tokens signed with RS256

### Service-to-Service
- Internal calls: Gateway not in path
- Option A: mTLS (future)
- Option B: Shared secret header `X-Service-Token` (current)
- Services validate `X-Service-Token` for internal endpoints

## Rationale
- **Single validation point**: Gateway = security perimeter; services stay simple
- **Performance**: No JWKS calls, no signature verification in each service
- **Spring Authorization Server**: Auth-server is a first-class service in the platform
- **Header-based auth**: Works with `@PreAuthorize`, `SecurityContext`, audit logging

## Consequences
- **Positive**: Low latency, simple services, centralized token revocation (gateway cache), clear trust boundary
- **Negative**: Gateway = single point of failure for auth; services trust network (mitigate with mTLS later)
- **Mitigation**: Gateway HA (multiple replicas), short token TTL (15min), header validation in services

## Alternatives Considered
- **Each service validates JWT**: Duplicated logic, JWKS calls per service, higher latency
- **Opaque tokens + introspection**: Extra network hop per request
- **mTLS everywhere**: Operational complexity, cert rotation, not needed for local dev
