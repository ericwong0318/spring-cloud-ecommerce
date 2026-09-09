# 03 — Auth Server

**What to build:** Spring Authorization Server issuing JWTs (RS256) at `http://localhost:9000`. Supports client_credentials and password grants. JWKS at `/oauth2/jwks`.

**Blocked by:** 01-config-server, 02-eureka-server

**Status:** ready-for-agent

- [ ] Auth Server starts on port 9000
- [ ] Registered clients: gateway, product, category, inventory, order, payment
- [ ] Token endpoint: `POST /oauth2/token` returns access_token (JWT)
- [ ] JWKS endpoint: `GET /oauth2/jwks` returns public keys
- [ ] Tokens contain: sub, roles, client_id, exp, iat
- [ ] PostgreSQL schema for registered clients, authorizations
- [ ] Actuator health + OpenAPI docs
- [ ] Dockerfile + docker-compose entry