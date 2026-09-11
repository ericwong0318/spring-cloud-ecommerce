# 15 — Platform: Config Server MongoDB Config for Product

**What to build:** Config Server Git repo contains `product.yml` with MongoDB connection settings for each profile (dev, docker, prod). Product service loads config at startup.

**Blocked by:** 14 — Platform: Docker Compose MongoDB Service

**Status:** done

- [x] Add `config-server/src/main/resources/config/product.yml` with:
  - `spring.data.mongodb.uri`, `spring.data.mongodb.database`
  - Profile-specific values: `dev` (localhost), `docker` (mongodb:27017), `prod` (managed URI)
- [x] Ensure product service `bootstrap.yml` imports config from Config Server (done via spring.cloud.config.import)
- [x] Verify product service starts in each profile and connects to correct MongoDB (via test profiles)
- [x] Verify no hardcoded MongoDB config in product service `application.yml` (only in config server)