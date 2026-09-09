# 04 — API Gateway

**What to build:** Spring Cloud Gateway at `http://localhost:8080`. Routes via `lb://service-name`. Validates JWT via auth-server JWKS. Forwards `X-User-Id`, `X-User-Roles`, `X-Client-Id` headers to downstream services.

**Blocked by:** 01-config-server, 02-eureka-server, 03-auth-server

**Status:** ready-for-agent

- [ ] Gateway starts on port 8080
- [ ] Routes: `/api/v1/products/**` → `lb://product`, `/api/v1/categories/**` → `lb://category`, `/api/v1/inventory/**` → `lb://inventory`, `/api/v1/orders/**` → `lb://order`, `/api/v1/payments/**` → `lb://payment`
- [ ] OAuth2 Resource Server: validates JWT from auth-server
- [ ] Extracts claims → forwards as headers: `X-User-Id`, `X-User-Roles`, `X-Client-Id`
- [ ] Service-to-service: validates `X-Service-Token` header for internal calls
- [ ] Rate limiting (basic)
- [ ] Actuator health + OpenAPI docs
- [ ] Dockerfile + docker-compose entry