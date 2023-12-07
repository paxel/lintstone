package paxel.lintstone.impl;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The GroupingExecutor class represents an executor service for grouping and executing tasks.
 * It provides methods for creating a sequential processor and managing the executor service.
 *
 * Usage example:
 *
 * GroupingExecutor executor = new GroupingExecutor();
 * SequentialProcessorBuilder builder = executor.create();
 * // Configure the sequential processor builder
 * SequentialProcessor processor = builder.build();
 * // Use the sequential processor to add and execute tasks
 *
 * // Shutdown the executor
 * executor.shutdown();
 *
 * Note: This implementation uses virtual threads for execution.
 */
public class GroupingExecutor {
    private final ExecutorService executorService;


    /**
     * The GroupingExecutor class represents an executor service for grouping and executing tasks.
     * It provides methods for creating a sequential processor and managing the executor service.
     */
    public GroupingExecutor() {
        // this implementation is completely for virtual Threads
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Creates a new SequentialProcessorBuilder instance.
     * The builder is used to configure the SequentialProcessor and submit the runnable to the executor service.
     *
     * @return the SequentialProcessorBuilder instance
     */
    public SequentialProcessorBuilder create() {
        // the Builder will submit the runnable to the service when the Processor is build.
        return new SequentialProcessorBuilder(executorService);
    }

    /**
     * Shuts down the executor service used by the GroupingExecutor.
     *
     * This method initiates an orderly shutdown of the executor service.
     * Any tasks that have been submitted to the executor service will be executed before the shutdown is complete.
     * The method returns immediately after initiating the shutdown, but it does not wait for the tasks to complete.
     * If you need to wait for the tasks to complete, use the awaitTermination method.
     *
     * @see GroupingExecutor#awaitTermination(long, TimeUnit)
     */
    public void shutdown() {
        executorService.shutdown();
    }

    /**
     * Shuts down the executor service immediately.
     *
     * This method initiates an immediate shutdown of the executor service.
     * Any tasks that have been submitted to the executor service may be terminated before completion.
     * The method returns immediately after initiating the shutdown, but it does not wait for the tasks to complete.
     *
     * @return a list of tasks that were scheduled for execution but have not yet started
     * @see GroupingExecutor#shutdown()
     * @see GroupingExecutor#awaitTermination(long, TimeUnit)
     */
    public List<Runnable> shutdownNow() {
        return executorService.shutdownNow();
    }

    /**
     * Returns true if the executor service used by the GroupingExecutor has been shut down, false otherwise.
     *
     * @return true if the executor service has been shut down, false otherwise
     */
    public boolean isShutdown() {
        return executorService.isShutdown();
    }

    /**
     * Returns true if the executor service used by the GroupingExecutor has been terminated, false otherwise.
     *
     * @return true if the executor service has been terminated, false otherwise
     */
    public boolean isTerminated() {
        return executorService.isTerminated();
    }

    /**
     * Waits for the executor service to terminate.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return true if the executor service terminated and false if the timeout elapsed before termination
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executorService.awaitTermination(timeout, unit);
    }

    @Override
    public String toString() {
        return "GroupingExecutor{" +
                "executorService=" + executorService +
                '}';
    }
}
