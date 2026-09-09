# 11 — Docker Compose (Local Dev)

**What to build:** Single `docker-compose.yml` that starts all 9 services + PostgreSQL (per service) + RabbitMQ + Config Server Git repo.

**Blocked by:** 01-config-server, 02-eureka-server, 03-auth-server, 04-gateway, 05-product-service, 06-category-service, 07-inventory-service, 08-payment-service, 09-order-service, 10-rabbitmq

**Status:** ready-for-agent

- [ ] `docker-compose.yml` at repo root
- [ ] Services: config-server, eureka-server, auth-server, gateway, product, category, inventory, order, payment
- [ ] Databases: 6 PostgreSQL instances (one per domain service + auth-server)
- [ ] RabbitMQ from ticket 10
- [ ] Config Server Git volume: `./config-server/config:/config`
- [ ] Network: all services on `ecommerce-network`
- [ ] Start order enforced via `depends_on` + healthchecks
- [ ] Environment variables for all service configs
- [ ] `docker-compose up -d` brings up full stack
- [ ] `docker-compose.test.yml` for CI (Testcontainers-compatible)