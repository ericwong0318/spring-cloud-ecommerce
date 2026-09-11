package com.example.system.load;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Utility for executing tasks in parallel with controlled concurrency.
 * Provides metrics collection and configurable thread pool management.
 */
public class ParallelExecutor {

    private static final Logger log = LoggerFactory.getLogger(ParallelExecutor.class);

    private final ExecutorService executor;
    private final int maxConcurrency;
    private final List<ExecutionResult<?>> results = new CopyOnWriteArrayList<>();

    public ParallelExecutor(int maxConcurrency) {
        this.maxConcurrency = maxConcurrency;
        this.executor = Executors.newFixedThreadPool(maxConcurrency, r -> {
            Thread t = new Thread(r, "load-test-" + Thread.currentThread().threadId());
            t.setDaemon(true);
            return t;
        });
    }

    public ParallelExecutor(int maxConcurrency, ThreadFactory threadFactory) {
        this.maxConcurrency = maxConcurrency;
        this.executor = Executors.newFixedThreadPool(maxConcurrency, threadFactory);
    }

    /**
     * Execute a supplier function in parallel with the specified number of tasks.
     * All tasks are submitted simultaneously and we wait for all to complete.
     *
     * @param taskSupplier the task to execute
     * @param taskCount number of parallel tasks to execute
     * @param <T> result type
     * @return list of execution results
     */
    public <T> List<ExecutionResult<T>> executeParallel(Supplier<T> taskSupplier, int taskCount) {
        return executeParallel(i -> taskSupplier.get(), taskCount);
    }

    /**
     * Execute a function with index in parallel.
     *
     * @param taskFunction function taking task index
     * @param taskCount number of parallel tasks
     * @param <T> result type
     * @return list of execution results
     */
    public <T> List<ExecutionResult<T>> executeParallel(Function<Integer, T> taskFunction, int taskCount) {
        results.clear();
        List<Future<ExecutionResult<T>>> futures = new ArrayList<>(taskCount);
        Instant startTime = Instant.now();

        for (int i = 0; i < taskCount; i++) {
            final int taskIndex = i;
            futures.add(executor.submit(() -> {
                Instant taskStart = Instant.now();
                try {
                    T result = taskFunction.apply(taskIndex);
                    Duration duration = Duration.between(taskStart, Instant.now());
                    return new ExecutionResult<>(true, result, null, duration, taskIndex);
                } catch (Exception e) {
                    Duration duration = Duration.between(taskStart, Instant.now());
                    return new ExecutionResult<>(false, null, e, duration, taskIndex);
                }
            }));
        }

        List<ExecutionResult<T>> completedResults = new ArrayList<>(taskCount);
        for (Future<ExecutionResult<T>> future : futures) {
            try {
                completedResults.add(future.get());
            } catch (InterruptedException | ExecutionException e) {
                completedResults.add(new ExecutionResult<>(false, null, e, Duration.ZERO, -1));
            }
        }

        Duration totalDuration = Duration.between(startTime, Instant.now());
        log.info("Completed {} parallel tasks in {} ms (max concurrency: {})",
                taskCount, totalDuration.toMillis(), maxConcurrency);

        results.addAll(completedResults);
        return completedResults;
    }

    /**
     * Execute tasks with a ramp-up period (gradually increasing concurrency).
     *
     * @param taskFunction function taking task index
     * @param taskCount total number of tasks
     * @param rampUpDuration duration to ramp up to full concurrency
     * @param <T> result type
     * @return list of execution results
     */
    public <T> List<ExecutionResult<T>> executeWithRampUp(Function<Integer, T> taskFunction,
                                                           int taskCount,
                                                           Duration rampUpDuration) {
        results.clear();
        List<Future<ExecutionResult<T>>> futures = new ArrayList<>(taskCount);
        Instant startTime = Instant.now();

        long delayPerTask = rampUpDuration.toMillis() / Math.max(1, taskCount);

        for (int i = 0; i < taskCount; i++) {
            final int taskIndex = i;
            futures.add(executor.submit(() -> {
                try {
                    Thread.sleep(taskIndex * delayPerTask);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                Instant taskStart = Instant.now();
                try {
                    T result = taskFunction.apply(taskIndex);
                    Duration duration = Duration.between(taskStart, Instant.now());
                    return new ExecutionResult<>(true, result, null, duration, taskIndex);
                } catch (Exception e) {
                    Duration duration = Duration.between(taskStart, Instant.now());
                    return new ExecutionResult<>(false, null, e, duration, taskIndex);
                }
            }));
        }

        List<ExecutionResult<T>> completedResults = new ArrayList<>(taskCount);
        for (Future<ExecutionResult<T>> future : futures) {
            try {
                completedResults.add(future.get());
            } catch (InterruptedException | ExecutionException e) {
                completedResults.add(new ExecutionResult<>(false, null, e, Duration.ZERO, -1));
            }
        }

        Duration totalDuration = Duration.between(startTime, Instant.now());
        log.info("Completed {} tasks with ramp-up in {} ms", taskCount, totalDuration.toMillis());

        results.addAll(completedResults);
        return completedResults;
    }

    /**
     * Execute tasks in batches.
     *
     * @param taskFunction function taking task index
     * @param taskCount total number of tasks
     * @param batchSize number of tasks per batch
     * @param delayBetweenBatches delay between batches
     * @param <T> result type
     * @return list of execution results
     */
    public <T> List<ExecutionResult<T>> executeInBatches(Function<Integer, T> taskFunction,
                                                          int taskCount,
                                                          int batchSize,
                                                          Duration delayBetweenBatches) {
        results.clear();
        List<ExecutionResult<T>> allResults = new ArrayList<>();

        for (int batchStart = 0; batchStart < taskCount; batchStart += batchSize) {
            int batchEnd = Math.min(batchStart + batchSize, taskCount);
            int batchCount = batchEnd - batchStart;

            List<Future<ExecutionResult<T>>> futures = new ArrayList<>(batchCount);
            for (int i = batchStart; i < batchEnd; i++) {
                final int taskIndex = i;
                futures.add(executor.submit(() -> {
                    Instant taskStart = Instant.now();
                    try {
                        T result = taskFunction.apply(taskIndex);
                        Duration duration = Duration.between(taskStart, Instant.now());
                        return new ExecutionResult<>(true, result, null, duration, taskIndex);
                    } catch (Exception e) {
                        Duration duration = Duration.between(taskStart, Instant.now());
                        return new ExecutionResult<>(false, null, e, duration, taskIndex);
                    }
                }));
            }

            List<ExecutionResult<T>> batchResults = new ArrayList<>(batchCount);
            for (Future<ExecutionResult<T>> future : futures) {
                try {
                    batchResults.add(future.get());
                } catch (InterruptedException | ExecutionException e) {
                    batchResults.add(new ExecutionResult<>(false, null, e, Duration.ZERO, -1));
                }
            }

            allResults.addAll(batchResults);

            if (batchEnd < taskCount && !delayBetweenBatches.isZero()) {
                try {
                    Thread.sleep(delayBetweenBatches.toMillis());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        results.addAll(allResults);
        return allResults;
    }

    /**
     * Get all execution results from the last run.
     */
    public List<ExecutionResult<?>> getResults() {
        return new ArrayList<>(results);
    }

    /**
     * Shutdown the executor service.
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Shutdown and wait for completion with timeout.
     */
    public boolean shutdown(long timeout, TimeUnit unit) {
        executor.shutdown();
        try {
            return executor.awaitTermination(timeout, unit);
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public int getMaxConcurrency() {
        return maxConcurrency;
    }

    /**
     * Result of a single task execution.
     */
    public static class ExecutionResult<T> {
        private final boolean success;
        private final T result;
        private final Exception exception;
        private final Duration duration;
        private final int taskIndex;

        public ExecutionResult(boolean success, T result, Exception exception, Duration duration, int taskIndex) {
            this.success = success;
            this.result = result;
            this.exception = exception;
            this.duration = duration;
            this.taskIndex = taskIndex;
        }

        public boolean isSuccess() {
            return success;
        }

        public T getResult() {
            return result;
        }

        public Exception getException() {
            return exception;
        }

        public Duration getDuration() {
            return duration;
        }

        public int getTaskIndex() {
            return taskIndex;
        }

        public long getDurationMillis() {
            return duration.toMillis();
        }
    }
}
