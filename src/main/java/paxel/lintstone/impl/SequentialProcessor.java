package paxel.lintstone.impl;

/**
 * Interface for a sequential task processor.
 */
public interface SequentialProcessor {
    /**
     * Adds a task to the processor.
     *
     * @param runnable the task to add.
     */
    void add(Runnable runnable);

    /**
     * Adds a task to the processor, blocking if the queue size exceeds the threshold.
     *
     * @param runnable       the task to add.
     * @param blockThreshold the queue size threshold.
     * @return {@code true} if the task was added.
     * @throws InterruptedException if interrupted while waiting.
     */
    boolean addWithBackPressure(Runnable runnable, int blockThreshold) throws InterruptedException;

    /**
     * Gets the number of queued tasks.
     *
     * @return the queue size.
     */
    int size();

    /**
     * Unregisters the processor gracefully.
     */
    void unregisterGracefully();

    /**
     * Shuts down the processor.
     *
     * @param now if {@code true}, shuts down immediately, discarding pending tasks.
     */
    void shutdown(boolean now);
}
