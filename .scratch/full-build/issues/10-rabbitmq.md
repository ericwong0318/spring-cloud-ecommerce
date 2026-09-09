# 10 — RabbitMQ Infrastructure

**What to build:** RabbitMQ cluster with management UI, configured for the e-commerce event topology.

**Blocked by:** None — can start immediately.

**Status:** ready-for-agent

- [ ] RabbitMQ starts on port 5672, management UI on 15672
- [ ] Exchange: `ecommerce.events` (topic, durable)
- [ ] Queues: `inventory.events`, `payment.events`, `order.events` (durable)
- [ ] DLX: `ecommerce.events.dlx` → `ecommerce.dlq`
- [ ] Bindings with routing keys: `order.*`, `inventory.*`, `payment.*`
- [ ] Publisher confirms enabled
- [ ] HA policy (mirrored queues) for production readiness
- [ ] Dockerfile + docker-compose entry
- [ ] Health check endpoint