# Load Test Documentation

This document describes how to run the high-throughput/load system tests locally and in CI.

## Overview

The load tests verify system behavior under concurrent load:
1. **Concurrent Order Placement (No Oversell)** - 200 parallel requests for 100 units
2. **Concurrent Reservation + Payment Capture** - 50 parallel orders with payment capture
3. **Scheduler Contention** - Multiple inventory-service instances processing expired reservations

## Prerequisites

- Docker/OrbStack running (for Testcontainers)
- Java 21
- Maven 3.9+
- At least 4GB RAM available for containers

## Running Load Tests Locally

### Option 1: Run All Load Tests

```bash
mvn test -pl system-test -Dtest=ECommerceSystemTest#testConcurrentOrderPlacementNoOversell,ECommerceSystemTest#testConcurrentReservationAndPaymentCapture,ECommerceSystemTest#testSchedulerContentionMultipleInstances
```

### Option 2: Run Individual Load Tests

```bash
# Test 1: Concurrent Order Placement - No Oversell
mvn test -pl system-test -Dtest=ECommerceSystemTest#testConcurrentOrderPlacementNoOversell

# Test 2: Concurrent Reservation + Payment Capture
mvn test -pl system-test -Dtest=ECommerceSystemTest#testConcurrentReservationAndPaymentCapture

# Test 3: Scheduler Contention
mvn test -pl system-test -Dtest=ECommerceSystemTest#testSchedulerContentionMultipleInstances
```

### Option 3: Run with More Resources

For heavier load tests, increase JVM heap and container resources:

```bash
MAVEN_OPTS="-Xmx4g" mvn test -pl system-test -Dtest=ECommerceSystemTest#testConcurrentOrderPlacementNoOversell
```

## Resource Requirements

| Test | Threads | Duration | Memory | CPU |
|------|---------|----------|--------|-----|
| Concurrent Order Placement | 50 | ~60s | ~2GB | 2-4 cores |
| Reservation + Payment Capture | 25 | ~90s | ~3GB | 2-4 cores |
| Scheduler Contention | 3 | ~60s | ~1GB | 2 cores |

**Total recommended**: 4GB RAM, 4 CPU cores

## CI Integration (Nightly)

Add to `.github/workflows/ci.yml` for nightly runs:

```yaml
name: Nightly Load Tests

on:
  schedule:
    - cron: '0 2 * * *'  # Run at 2 AM UTC daily

jobs:
  load-tests:
    runs-on: ubuntu-latest
    timeout-minutes: 30
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Start Docker
        uses: docker-practice/actions-setup-docker@v1
      - name: Run Load Tests
        run: |
          mvn test -pl system-test \
            -Dtest=ECommerceSystemTest#testConcurrentOrderPlacementNoOversell,ECommerceSystemTest#testConcurrentReservationAndPaymentCapture,ECommerceSystemTest#testSchedulerContentionMultipleInstances \
            -DfailIfNoTests=false
      - name: Upload Test Results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: load-test-results
          path: system-test/target/surefire-reports/
```

## Test Configuration

### ParallelExecutor Configuration

The `ParallelExecutor` utility accepts concurrency settings:

```java
// High concurrency for order placement
ParallelExecutor executor = new ParallelExecutor(50);

// Medium concurrency for payment operations
ParallelExecutor executor = new ParallelExecutor(25);

// Low concurrency for scheduler simulation
ParallelExecutor executor = new ParallelExecutor(3);
```

### Metrics Collection

All tests use `MetricsCollector` which provides:
- Throughput (ops/sec)
- Latency percentiles (p50, p95, p99)
- Error rates
- Exception categorization

Metrics are printed to console and can be redirected to files for analysis.

## Test Details

### 1. Concurrent Order Placement (No Oversell)

**Scenario**: 200 parallel requests, each ordering 1 unit from inventory of 100.

**Assertions**:
- Exactly 100 orders succeed (CONFIRMED)
- Exactly 100 orders fail (insufficient stock)
- Final inventory: quantity=100, reserved=0, available=0
- No negative quantities
- No lost updates

**Expected Errors**: HTTP 409 Conflict (insufficient stock)

### 2. Concurrent Reservation + Payment Capture

**Scenario**: 50 parallel orders → authorize → capture all in parallel.

**Assertions**:
- All 50 orders reach RESERVED state
- All 50 payments authorized
- All 50 captures succeed (idempotent)
- Final inventory: quantity=0, reserved=0, available=0
- Exactly 50 CONFIRMED orders
- Exactly 50 CAPTURED events (no duplicates)

### 3. Scheduler Contention

**Scenario**: 3 inventory-service instances process 50 expired reservations concurrently.

**Assertions**:
- Exactly 50 reservations processed (not 150)
- Advisory locks prevent duplicate processing
- Exactly 50 RELEASED events published
- No deadlocks or lock timeouts
- Error rate = 0%

## Debugging Failed Load Tests

### Common Issues

1. **Testcontainers not starting**: Ensure Docker/OrbStack is running
2. **Port conflicts**: Tests use random ports, but ensure 5432, 5672, 27017 not in use
3. **Timeouts**: Increase `Awaitility` timeouts in test if system is slow
4. **Memory**: Increase `-Xmx` if OOM errors occur

### Viewing Metrics

Tests print metrics summary to console. For detailed analysis:

```bash
# Redirect to file
mvn test -pl system-test -Dtest=ECommerceSystemTest#testConcurrentOrderPlacementNoOversell 2>&1 | tee load-test-results.log
```

### Analyzing Errors

Error breakdown is logged:
```
=== Error Breakdown for ConcurrentOrderPlacement ===
Total errors: 100
  HttpClientErrorException$Conflict: 100
```

## Extending Load Tests

### Adding New Load Test

1. Create test method in `ECommerceSystemTest` with `@Order` annotation
2. Use `ParallelExecutor` for concurrency
3. Use `MetricsCollector` for metrics
4. Use `AssertionHelpers` for assertions
5. Add `@DisplayName` with "LOAD:" prefix

### Customizing Concurrency

```java
// Ramp-up execution
executor.executeWithRampUp(taskFunction, taskCount, Duration.ofSeconds(30));

// Batch execution
executor.executeInBatches(taskFunction, taskCount, 10, Duration.ofSeconds(5));
```

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Connection refused | Check Testcontainers started, ports exposed |
| Deadlock in scheduler | Verify advisory lock implementation in inventory-service |
| Flaky tests | Increase await timeouts, check resource limits |
| OOM | Increase Maven/JVM heap: `MAVEN_OPTS="-Xmx4g"` |

## Related Files

- `system-test/src/test/java/com/example/system/load/ParallelExecutor.java`
- `system-test/src/test/java/com/example/system/load/MetricsCollector.java`
- `system-test/src/test/java/com/example/system/load/AssertionHelpers.java`
- `system-test/src/test/java/com/example/system/ECommerceSystemTest.java` (load test methods)
