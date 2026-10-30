# Incident Runbook

On-call runbook for the Spring Cloud Microservices Platform. Services start in this order:

**Config Server (8888) → Eureka (8761) → Auth Server (9000) → Gateway (8080) → Business services (8081/8082/8083/8084/8086/8087)**

Verify health before proceeding: `curl http://localhost:<port>/actuator/health`

---

## 1. Circuit breaker tripping (Resilience4j)

**Affected services:** Gateway, Order Service, Inventory Service, Payment Service

### Symptoms
- Calls to downstream services fail fast with `CallNotPermittedException`
- Elevated error rate on gateway routes (e.g. `/api/v1/orders`)
- Resilience4j metrics show `circuitbreaker.calls.failed` increasing, state = `OPEN`

### Causes
- Downstream service unhealthy or slow (Auth Server, Product, Payment, Inventory)
- Network partition between services
- RabbitMQ or database unavailable for dependent service

### Remediation
1. Check downstream service health:
   ```bash
   curl http://localhost:8083/actuator/health   # order-service
   curl http://localhost:8084/actuator/health   # inventory-service
   curl http://localhost:8086/actuator/health   # payment-service
   curl http://localhost:9000/actuator/health   # auth-server
   ```
2. Check Resilience4j metrics:
   ```bash
   curl http://localhost:<service-port>/actuator/metrics/resilience4j.circuitbreaker.calls.failed
   ```
3. If downstream is down, restart it in startup order (see above).
4. Circuit breaker transitions `OPEN → HALF_OPEN → CLOSED` automatically after the wait duration. To force a reset, restart the affected service.
5. Review `resilience4j.circuitbreaker` config in `config-server/src/main/resources/config/<service>.yml` if trips are frequent.

### Prevention
- Tune `slidingWindowSize`, `failureRateThreshold`, `waitDurationInOpenState` per service in Config Server.
- Add alerting on `circuitbreaker.calls.failed` rate via Prometheus/Splunk.

---

## 2. RabbitMQ DLX replay (failed messages)

**Affected services:** all domain services (order, inventory, payment, category, notification)

### Symptoms
- Events not consumed (orders not transitioning, reservations not released, payments not captured)
- Messages accumulating in `ecommerce.events.dlx` (dead letter exchange)
- Consumer lag growing

### Causes
- Consumer threw an exception and message was rejected/requeued to DLX
- Transient database or downstream service failure during event handling
- Poison message (malformed payload) that always fails

### Remediation
1. Inspect the DLX queue:
   ```bash
   # via RabbitMQ Management UI (http://localhost:15672)
   # or rabbitmqadmin
   rabbitmqadmin list queues name consumers messages
   ```
2. Identify the failing consumer and check its logs:
   ```bash
   # order-service example
   kubectl logs -n ecommerce -l app=order-service --tail=100
   ```
3. Fix the underlying cause (database connectivity, downstream service, bad payload).
4. Replay DLX messages:
   - **Option A:** Move messages from DLX back to the original queue via RabbitMQ Management UI (requeue/republish).
   - **Option B:** Purge the DLX if messages are poison/purgeable, then re-trigger the business event (e.g. re-place the order).
5. Verify the consumer processes the replayed messages and DLX depth returns to zero.

### Prevention
- Consumers use idempotency tables (`processed_events`) so replays are safe.
- Add alerting on DLX queue depth > 0.
- Ensure all event handlers catch exceptions and log the full payload for debugging.

---

## 3. Reservation expiry remediation

**Affected services:** Inventory Service, Order Service

### Symptoms
- Orders stuck in `RESERVED` state past the reservation TTL
- Inventory shows `RESERVED` stock that is never released
- Payment capture never happens for expired reservations

### Configuration
- Reservation TTL: **15 minutes** (see `docs/architecture/event-driven-architecture.md`, `saga.reservation-ttl: 15min`)
- `ReservationExpiryScheduler` runs every **60 seconds** and publishes `ReservationExpiredEvent` for orders in `RESERVED` with items reserved > 15 minutes
- Order Service transitions the order to `CANCELLED` on `ReservationExpiredEvent`

### Remediation
1. Check the scheduler is running in order-service logs:
   ```bash
   kubectl logs -n ecommerce -l app=order-service | grep -i reservationexpiryscheduler
   ```
2. Verify `ReservationExpiredEvent` is published to RabbitMQ:
   ```bash
   rabbitmqadmin list queues name consumers messages  # look for reservation.expired.queue
   ```
3. Verify Order Service consumed the event and transitioned the order to `CANCELLED`.
4. If scheduler missed a window, manually expire: query orders in `RESERVED` > 15 min and publish `ReservationExpiredEvent` for each, or transition orders to `CANCELLED` directly via database/order-service endpoint.
5. Release the corresponding inventory reservations (inventory-service transitions `RESERVED → AVAILABLE`).

### Prevention
- Alert on orders stuck in `RESERVED` > 15 minutes.
- Alert if `ReservationExpiryScheduler` hasn't run in > 5 minutes.
- Monitor `reservation.expired.queue` depth.

---

## 4. Service startup order troubleshooting

### Symptoms
- `Connection refused` to Config Server
- `No instances available` in Eureka dashboard
- `401 Unauthorized` at Gateway
- `Could not resolve placeholder` errors at service startup

### Correct order
```
Config Server (8888)
  → Eureka Server (8761)
  → Auth Server (9000)
  → Gateway (8080)
  → product (8081), category (8082), order-service (8083),
    inventory-service (8084), payment-service (8086),
    notification-service (8087)
```

### Remediation
1. Start Config Server first and verify: `curl http://localhost:8888/actuator/health`
2. Start Eureka and verify: `curl http://localhost:8761/actuator/health`
3. Check Eureka registry: `curl http://localhost:8761/eureka/apps`
4. Start Auth Server, then Gateway, then business services — waiting for each health check to pass before the next.
5. If a service fails with `Could not resolve placeholder`, ensure `SPRING_CLOUD_CONFIG_URI` points to a healthy Config Server and `SPRING_PROFILES_ACTIVE` is set (`dev`, `docker`, or `prod`).

---

## 5. Testcontainers / Docker environment issues

**Affected:** integration tests (`*IntegrationTest.java`), system-test module, load tests

### Symptoms
- `Could not find a valid Docker environment`
- `Ryuk container failed to start`
- `Connection refused to unix:///var/run/docker.sock`
- Test timeouts

### Remediation
1. Verify Docker/OrbStack is running: `docker version` / `docker ps`
2. On OrbStack (macOS), ensure Settings → General → "Expose Docker socket" is enabled.
3. Test Testcontainers connection:
   ```bash
   docker run --rm testcontainers/ryuk:0.12.0
   ```
4. Increase Docker resources (memory ≥ 4GB) if Ryuk fails.
5. Disable Ryuk cleanup if needed:
   ```bash
   export TESTCONTAINERS_RYUK_DISABLED=true
   ```
6. Ensure ports 5432, 5672, 27017 are not already in use by local databases/brokers.
7. Increase JVM heap for memory-heavy load tests:
   ```bash
   MAVEN_OPTS="-Xmx4g" mvn test -pl system-test -Dtest=ECommerceSystemTest#<test>
   ```

---

## 6. Escalation

If the incident is not resolved by the steps above:

1. Gather logs: `kubectl logs -n ecommerce -l app=<service>` or local `logs/<service>.log`
2. Gather metrics: Splunk Observability Cloud (JVM metrics, traces, logs via OTel)
3. Contact the platform team with:
   - Service(s) affected
   - Time window
   - Error messages and stack traces
   - Steps already attempted
