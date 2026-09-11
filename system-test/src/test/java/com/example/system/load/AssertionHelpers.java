package com.example.system.load;

import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.inventory.model.Inventory;
import com.example.inventory.repository.InventoryRepository;
import com.example.order.repository.OrderRepository;
import com.example.order.repository.OrderItemRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Assertion helpers for load tests.
 * Provides specialized assertions for verifying system behavior under load.
 */
public class AssertionHelpers {

    private static final Logger log = LoggerFactory.getLogger(AssertionHelpers.class);

    /**
     * Assert that exactly N orders succeeded and M failed.
     */
    public static <T> void assertOrderSuccessCount(List<ParallelExecutor.ExecutionResult<T>> results,
                                                    int expectedSuccess, int expectedFailure) {
        long successCount = results.stream().filter(ParallelExecutor.ExecutionResult::isSuccess).count();
        long failureCount = results.stream().filter(r -> !r.isSuccess()).count();

        Assertions.assertThat(successCount)
                .as("Expected %d successful orders but got %d", expectedSuccess, successCount)
                .isEqualTo(expectedSuccess);

        Assertions.assertThat(failureCount)
                .as("Expected %d failed orders but got %d", expectedFailure, failureCount)
                .isEqualTo(expectedFailure);
    }

    /**
     * Assert that exactly N payments succeeded.
     */
    public static <T> void assertPaymentSuccessCount(List<ParallelExecutor.ExecutionResult<T>> results, int expectedSuccess) {
        long successCount = results.stream().filter(ParallelExecutor.ExecutionResult::isSuccess).count();

        Assertions.assertThat(successCount)
                .as("Expected %d successful payments but got %d", expectedSuccess, successCount)
                .isEqualTo(expectedSuccess);
    }

    /**
     * Wait for inventory to reach expected state.
     */
    public static void assertInventoryState(InventoryRepository inventoryRepository,
                                             Long variantId,
                                             int expectedQuantity,
                                             int expectedReserved,
                                             int expectedAvailable) {
        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    Inventory inventory = inventoryRepository.findByVariantId(variantId)
                            .orElseThrow(() -> new AssertionError("Inventory not found for variant: " + variantId));

                    Assertions.assertThat(inventory.getQuantity())
                            .as("Total quantity mismatch for variant %d", variantId)
                            .isEqualTo(expectedQuantity);

                    Assertions.assertThat(inventory.getReservedQuantity())
                            .as("Reserved quantity mismatch for variant %d", variantId)
                            .isEqualTo(expectedReserved);

                    Assertions.assertThat(inventory.getAvailableQuantity())
                            .as("Available quantity mismatch for variant %d", variantId)
                            .isEqualTo(expectedAvailable);
                });
    }

    /**
     * Assert no oversell occurred (available quantity never negative).
     */
    public static void assertNoOversell(InventoryRepository inventoryRepository, Long variantId) {
        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    Inventory inventory = inventoryRepository.findByVariantId(variantId)
                            .orElseThrow(() -> new AssertionError("Inventory not found for variant: " + variantId));

                    Assertions.assertThat(inventory.getAvailableQuantity())
                            .as("Available quantity should never be negative (oversell detected)")
                            .isGreaterThanOrEqualTo(0);

                    Assertions.assertThat(inventory.getReservedQuantity())
                            .as("Reserved quantity should never be negative")
                            .isGreaterThanOrEqualTo(0);

                    Assertions.assertThat(inventory.getReservedQuantity())
                            .as("Reserved quantity should not exceed total quantity")
                            .isLessThanOrEqualTo(inventory.getQuantity());
                });
    }

    /**
     * Assert that order count in database matches expected.
     */
    public static void assertOrderCount(OrderRepository orderRepository, long expectedCount) {
        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    long actualCount = orderRepository.count().block();
                    Assertions.assertThat(actualCount)
                            .as("Expected %d orders in database but found %d", expectedCount, actualCount)
                            .isEqualTo(expectedCount);
                });
    }

    /**
     * Assert that confirmed order count matches expected.
     */
    public static void assertConfirmedOrderCount(OrderRepository orderRepository, long expectedCount) {
        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    // This would need a custom query - for now check all orders
                    long confirmedCount = orderRepository.findAll()
                            .collectList()
                            .block()
                            .stream()
                            .filter(o -> "CONFIRMED".equals(o.getStatus()))
                            .count();
                    Assertions.assertThat(confirmedCount)
                            .as("Expected %d CONFIRMED orders but found %d", expectedCount, confirmedCount)
                            .isEqualTo(expectedCount);
                });
    }

    /**
     * Assert idempotency: same operation with same idempotency key produces same result.
     */
    public static <T> void assertIdempotentResults(List<ParallelExecutor.ExecutionResult<T>> results,
                                                    java.util.function.Function<T, Object> resultExtractor) {
        Map<Object, AtomicInteger> resultCounts = new ConcurrentHashMap<>();

        results.stream()
                .filter(ParallelExecutor.ExecutionResult::isSuccess)
                .map(ParallelExecutor.ExecutionResult::getResult)
                .map(resultExtractor)
                .forEach(key -> resultCounts.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet());

        for (Map.Entry<Object, AtomicInteger> entry : resultCounts.entrySet()) {
            Assertions.assertThat(entry.getValue().get())
                    .as("Idempotency violation: result %s returned %d times", entry.getKey(), entry.getValue().get())
                    .isEqualTo(1);
        }
    }

    /**
     * Assert that scheduler processed exactly N expirations (not N * instances).
     */
    public static void assertSchedulerProcessedOnce(InventoryRepository inventoryRepository,
                                                     List<Long> variantIds,
                                                     int expectedTotalReleased) {
        Awaitility.await()
                .atMost(Duration.ofMinutes(2))
                .pollInterval(Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    int totalReleased = variantIds.stream()
                            .mapToInt(variantId -> {
                                Inventory inv = inventoryRepository.findByVariantId(variantId).orElseThrow();
                                return inv.getQuantity() - inv.getAvailableQuantity();
                            })
                            .sum();

                    // Total confirmed = total released (reservations expired and released)
                    Assertions.assertThat(totalReleased)
                            .as("Expected %d total released units but got %d (indicates duplicate processing)",
                                    expectedTotalReleased, totalReleased)
                            .isEqualTo(expectedTotalReleased);
                });
    }

    /**
     * Assert that all events were processed exactly once (no duplicates).
     */
    public static void assertNoDuplicateEvents(Map<String, AtomicInteger> eventCounts,
                                                int expectedCountPerType) {
        for (Map.Entry<String, AtomicInteger> entry : eventCounts.entrySet()) {
            Assertions.assertThat(entry.getValue().get())
                    .as("Event type %s: expected %d occurrences but got %d (possible duplicate)",
                            entry.getKey(), expectedCountPerType, entry.getValue().get())
                    .isEqualTo(expectedCountPerType);
        }
    }

    /**
     * Assert throughput meets minimum requirement.
     */
    public static void assertMinimumThroughput(MetricsCollector metricsCollector,
                                                String operation,
                                                double minOpsPerSecond) {
        MetricsCollector.AggregatedMetrics metrics = metricsCollector.getMetrics(operation);

        Assertions.assertThat(metrics.throughputPerSecond())
                .as("Throughput for %s: expected at least %.2f ops/sec but got %.2f",
                        operation, minOpsPerSecond, metrics.throughputPerSecond())
                .isGreaterThanOrEqualTo(minOpsPerSecond);
    }

    /**
     * Assert latency percentiles meet SLA.
     */
    public static void assertLatencySLA(MetricsCollector metricsCollector,
                                         String operation,
                                         double maxP95Ms,
                                         double maxP99Ms) {
        MetricsCollector.AggregatedMetrics metrics = metricsCollector.getMetrics(operation);

        Assertions.assertThat(metrics.p95LatencyMs())
                .as("P95 latency for %s: expected <= %.2f ms but got %.2f",
                        operation, maxP95Ms, metrics.p95LatencyMs())
                .isLessThanOrEqualTo(maxP95Ms);

        Assertions.assertThat(metrics.p99LatencyMs())
                .as("P99 latency for %s: expected <= %.2f ms but got %.2f",
                        operation, maxP99Ms, metrics.p99LatencyMs())
                .isLessThanOrEqualTo(maxP99Ms);
    }

    /**
     * Assert error rate is below threshold.
     */
    public static void assertErrorRateBelow(MetricsCollector metricsCollector,
                                             String operation,
                                             double maxErrorRate) {
        MetricsCollector.AggregatedMetrics metrics = metricsCollector.getMetrics(operation);

        Assertions.assertThat(metrics.errorRate())
                .as("Error rate for %s: expected <= %.2f%% but got %.2f%%",
                        operation, maxErrorRate * 100, metrics.errorRate() * 100)
                .isLessThanOrEqualTo(maxErrorRate);
    }

    /**
     * Wait for a condition with load-test appropriate timeout.
     */
    public static void awaitCondition(String description, Callable<Boolean> condition) {
        Awaitility.await()
                .atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofMillis(500))
                .with()
                .pollDelay(Duration.ZERO)
                .until(condition);
    }

    /**
     * Collect results from parallel execution and categorize by exception type.
     */
    public static <T> Map<String, Long> categorizeErrors(List<ParallelExecutor.ExecutionResult<T>> results) {
        return results.stream()
                .filter(r -> !r.isSuccess() && r.getException() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getException().getClass().getSimpleName(),
                        Collectors.counting()
                ));
    }

    /**
     * Log error categorization for debugging.
     */
    public static <T> void logErrorBreakdown(List<ParallelExecutor.ExecutionResult<T>> results, String testName) {
        Map<String, Long> errorCounts = categorizeErrors(results);
        long totalErrors = errorCounts.values().stream().mapToLong(Long::longValue).sum();

        log.info("=== Error Breakdown for {} ===", testName);
        log.info("Total errors: {}", totalErrors);
        errorCounts.forEach((type, count) ->
                log.info("  {}: {}", type, count));
    }
}