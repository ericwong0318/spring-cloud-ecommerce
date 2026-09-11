package com.example.system.load;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Collects and aggregates metrics from load test executions.
 * Provides statistics like throughput, latency percentiles, error rates.
 */
public class MetricsCollector {

    private static final Logger log = LoggerFactory.getLogger(MetricsCollector.class);

    private final ConcurrentMap<String, MetricSeries> metrics = new ConcurrentHashMap<>();
    private final Instant startTime = Instant.now();

    /**
     * Record a successful operation.
     */
    public void recordSuccess(String operation, Duration latency) {
        MetricSeries series = metrics.computeIfAbsent(operation, MetricSeries::new);
        series.recordSuccess(latency);
    }

    /**
     * Record a failed operation.
     */
    public void recordFailure(String operation, Duration latency, Exception exception) {
        MetricSeries series = metrics.computeIfAbsent(operation, MetricSeries::new);
        series.recordFailure(latency, exception);
    }

    /**
     * Record a generic value (e.g., queue size, connection count).
     */
    public void recordValue(String metric, double value) {
        MetricSeries series = metrics.computeIfAbsent(metric, MetricSeries::new);
        series.recordValue(value);
    }

    /**
     * Get aggregated metrics for an operation.
     */
    public AggregatedMetrics getMetrics(String operation) {
        MetricSeries series = metrics.get(operation);
        return series != null ? series.aggregate() : new AggregatedMetrics(operation, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, List.of());
    }

    /**
     * Get all aggregated metrics.
     */
    public List<AggregatedMetrics> getAllMetrics() {
        return metrics.values().stream()
                .map(MetricSeries::aggregate)
                .collect(Collectors.toList());
    }

    /**
     * Print summary to log.
     */
    public void printSummary() {
        Duration totalDuration = Duration.between(startTime, Instant.now());
        log.info("=== Load Test Metrics Summary (duration: {} ms) ===", totalDuration.toMillis());

        for (AggregatedMetrics m : getAllMetrics()) {
            log.info("Operation: {}", m.operation());
            log.info("  Total: {} | Success: {} | Failed: {} | Error Rate: {:.2f}%",
                    m.totalCount(), m.successCount(), m.failureCount(), m.errorRate() * 100);
            log.info("  Latency (ms): min={} | max={} | avg={:.2f} | p50={} | p95={} | p99={}",
                    m.minLatencyMs(), m.maxLatencyMs(), m.avgLatencyMs(),
                    m.p50LatencyMs(), m.p95LatencyMs(), m.p99LatencyMs());
            log.info("  Throughput: {:.2f} ops/sec", m.throughputPerSecond());
        }
    }

    /**
     * Reset all metrics.
     */
    public void reset() {
        metrics.clear();
    }

    /**
     * Internal metric series for a single operation.
     */
    private static class MetricSeries {
        private final String name;
        private final AtomicLong successCount = new AtomicLong(0);
        private final AtomicLong failureCount = new AtomicLong(0);
        private final AtomicLong totalLatencyNanos = new AtomicLong(0);
        private final AtomicLong minLatencyNanos = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong maxLatencyNanos = new AtomicLong(0);
        private final List<Long> latencies = new ArrayList<>();
        private final List<Exception> exceptions = new ArrayList<>();

        public MetricSeries(String name) {
            this.name = name;
        }

        public void recordSuccess(Duration latency) {
            successCount.incrementAndGet();
            long nanos = latency.toNanos();
            totalLatencyNanos.addAndGet(nanos);
            updateMinMax(nanos);
            synchronized (latencies) {
                latencies.add(nanos);
            }
        }

        public void recordFailure(Duration latency, Exception exception) {
            failureCount.incrementAndGet();
            long nanos = latency.toNanos();
            totalLatencyNanos.addAndGet(nanos);
            updateMinMax(nanos);
            synchronized (latencies) {
                latencies.add(nanos);
            }
            synchronized (exceptions) {
                exceptions.add(exception);
            }
        }

        public void recordValue(double value) {
            // For generic values, we just store them
            synchronized (latencies) {
                latencies.add((long) value);
            }
        }

        private void updateMinMax(long nanos) {
            minLatencyNanos.updateAndGet(current -> Math.min(current, nanos));
            maxLatencyNanos.updateAndGet(current -> Math.max(current, nanos));
        }

        public AggregatedMetrics aggregate() {
            long total = successCount.get() + failureCount.get();
            if (total == 0) {
                return new AggregatedMetrics(name, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, List.of());
            }

            List<Long> sortedLatencies;
            synchronized (latencies) {
                sortedLatencies = latencies.stream().sorted().collect(Collectors.toList());
            }

            long minNs = minLatencyNanos.get() == Long.MAX_VALUE ? 0 : minLatencyNanos.get();
            long maxNs = maxLatencyNanos.get();
            double avgNs = totalLatencyNanos.get() / (double) total;

            return new AggregatedMetrics(
                    name,
                    total,
                    successCount.get(),
                    failureCount.get(),
                    (double) failureCount.get() / total,
                    minNs / 1_000_000.0,
                    maxNs / 1_000_000.0,
                    avgNs / 1_000_000.0,
                    percentile(sortedLatencies, 0.50) / 1_000_000.0,
                    percentile(sortedLatencies, 0.95) / 1_000_000.0,
                    percentile(sortedLatencies, 0.99) / 1_000_000.0,
                    exceptions.stream().map(Throwable::getClass).map(Class::getSimpleName).collect(Collectors.toList())
            );
        }

        private long percentile(List<Long> sorted, double p) {
            if (sorted.isEmpty()) return 0;
            int index = (int) Math.ceil(p * sorted.size()) - 1;
            return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
        }
    }

    /**
     * Aggregated metrics for an operation.
     */
    public record AggregatedMetrics(
            String operation,
            long totalCount,
            long successCount,
            long failureCount,
            double errorRate,
            double minLatencyMs,
            double maxLatencyMs,
            double avgLatencyMs,
            double p50LatencyMs,
            double p95LatencyMs,
            double p99LatencyMs,
            List<String> exceptionTypes
    ) {
        public double throughputPerSecond() {
            // This would need total test duration - simplified here
            return successCount / Math.max(1, (avgLatencyMs / 1000.0) * totalCount);
        }
    }
}
