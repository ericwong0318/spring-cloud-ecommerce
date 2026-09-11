# 02 — Debezium CDC Setup for Event Publishing

**Status:** **superseded** — replaced by [02b — Event-Driven Reservation & Cancellation Flow (RabbitMQ)](./02b-event-driven-reservation-cancellation-rabbitmq.md)

The domain modeling grilling session resolved on **RabbitMQ direct publishing** instead of Debezium CDC + outbox pattern. This ticket is retained for history.

---

~~**What to build:** Debezium connector configured for PostgreSQL (all services). Captures INSERT/UPDATE/DELETE on outbox tables → publishes to Kafka/RabbitMQ. Replaces transactional outbox / publisher confirms. Includes connector config, schema registry, offset management.~~

~~**Blocked by:** 01 — Event Infrastructure: `eventId` + Idempotency Foundation~~

~~**Status:** ready-for-agent~~

~~- [ ] Add Debezium PostgreSQL connector dependency to each service (or shared config module)~~
~~- [ ] Create outbox table pattern: `event_outbox(id, aggregate_type, aggregate_id, event_type, payload, created_at)` in each service's DB~~
~~- [ ] Configure Debezium connector: PostgreSQL source, Kafka/RabbitMQ sink, topic routing (e.g., `inventory-service.inventory` → `inventory.events`)~~
~~- [ ] Set up schema registry (Avro/JSON) for event payloads~~
~~- [ ] Configure offset storage (Kafka topic or DB) for exactly-once semantics~~
~~- [ ] Add connector health checks + monitoring (lag metrics)~~
~~- [ ] Document: how services write to outbox table within same transaction as domain changes~~
~~- [ ] Integration test: write to outbox → verify event appears in Kafka/RabbitMQ with correct `eventId`~~
