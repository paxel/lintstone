package paxel.lintstone.api;

import paxel.lintstone.impl.SequentialProcessorBuilder;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Factory for creating sequential processors.
 */
public interface ProcessorFactory {
    /**
     * Creates a new sequential processor builder.
     *
     * @return a new builder.
     */
    SequentialProcessorBuilder create();

    /**
     * Shuts down the factory and its underlying resources.
     */
    void shutdown();

    /**
     * Shuts down the factory immediately and returns the list of pending tasks.
     *
     * @return the list of runnables that were never executed.
     */
    List<Runnable> shutdownNow();

    /**
     * Checks if the factory has been shut down.
     *
     * @return {@code true} if shut down.
     */
    boolean isShutdown();

    /**
     * Waits for the factory to terminate.
     *
     * @param timeout the maximum time to wait.
     * @param unit    the time unit of the timeout argument.
     * @return {@code true} if terminated, {@code false} if the timeout elapsed before termination.
     * @throws InterruptedException if interrupted while waiting.
     */
    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;
}
