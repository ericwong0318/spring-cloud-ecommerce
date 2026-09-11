# 14 — Platform: Docker Compose MongoDB Service

**What to build:** `docker-compose.yml` (and `docker-compose.test.yml`) includes MongoDB service for product service. PostgreSQL remains for inventory, order, payment, category, auth.

**Blocked by:** 06 — Product: POM Cleanup (need MongoDB for product integration tests)

**Status:** done

- [x] Add `mongodb` service to `docker-compose.yml`: image `mongo:7`, port 27017, volume for persistence
- [x] Add `mongodb` to `docker-compose.test.yml` for Testcontainers integration tests
- [x] Configure product service `spring.data.mongodb.uri` to use `mongodb://mongodb:27017/productdb` in docker profile
- [ ] Verify `docker-compose up -d` starts MongoDB + all services
- [ ] Verify product service connects to MongoDB in docker profile
- [ ] Verify existing PostgreSQL services still start and connect
