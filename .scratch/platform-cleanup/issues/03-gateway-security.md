# 03 — Gateway Security Hardening

**What to build:** Add proper OAuth2 resource server configuration to the API Gateway including token relay, route-level authorization, and rate limiting.

**Blocked by:** None — can start immediately.

**Status:** done

- [x] Create `SecurityConfig.java` in gateway module with `SecurityFilterChain` bean
- [x] Enable `TokenRelayGatewayFilterFactory` to forward JWT tokens to downstream services
- [x] Configure route-specific security: public routes (health, actuator) vs protected routes
- [x] Add rate limiting using `RequestRateLimiterGatewayFilterFactory` with Redis or in-memory backend
- [x] Configure CORS for gateway
- [x] Add JWT validation (issuer, audience, claims) matching auth-server configuration
- [x] Test: verify tokens issued by auth-server (`http://localhost:9000/oauth2/token`) work through gateway to services
- [x] Run gateway tests: `mvn test -pl gateway`
