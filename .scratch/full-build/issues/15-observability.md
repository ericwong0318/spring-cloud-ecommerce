# 15 — Observability Stack

**What to build:** Structured logging, distributed tracing (Micrometer + OpenTelemetry), Prometheus metrics, correlation ID propagation.

**Blocked by:** 04-gateway, 05-product-service, 06-category-service, 07-inventory-service, 08-payment-service, 09-order-service

**Status:** ready-for-agent

- [ ] All services: JSON logging with `correlationId` (from `X-Correlation-Id` header)
- [ ] Gateway: generates `X-Correlation-Id` if missing, propagates to all downstream calls
- [ ] Micrometer + Prometheus registry on all services (`/actuator/prometheus`)
- [ ] OpenTelemetry tracing: spans for HTTP client calls, RabbitMQ publish/consume
- [ ] OTLP exporter configured (stdout for local, collector for prod)
- [ ] Custom metrics: order.created, order.completed, payment.authorized, inventory.reserved
- [ ] Grafana dashboards (JSON) for: request latency, error rates, saga duration, queue depths