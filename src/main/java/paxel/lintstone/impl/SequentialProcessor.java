package paxel.lintstone.impl;

/**
 * A SequentialProcessor interface for managing sequential processing of Runnables.
 */
public interface SequentialProcessor {

    /**
     * Adds a {@link Runnable} to be processed.
     *
     * @param runnable The {@link Runnable} to be added.
     */
    void add(Runnable runnable);

    /**
     * Adds a {@link Runnable} to be processed with back pressure.
     *
     * @param runnable       The {@link Runnable} to be added.
     * @param blockThreshold The threshold for back pressure. If the number of currently queued messages in the SequentialProcessor exceeds this threshold, the message will be rejected
     *.
     * @return true if the runnable was successfully added, false if the SequentialProcessor rejected the message.
     */
    boolean addWithBackPressure(Runnable runnable, Integer blockThreshold);

    /**
     * Returns the number of messages currently queued in the SequentialProcessor.
     *
     * @return The number of queued messages.
     */
    int size();

    /**
     * Unregisters the actor gracefully. This method sets the 'registered' flag to false
     * and calls the unregisterGracefully() method of the sequential processor associated with the actor.
     */
    void unregisterGracefully();

    /**
     * Shuts down the SequentialProcessor.
     *
     * @param now true if the shutdown should happen immediately, false if it should be graceful
     */
    void shutdown(boolean now);
}
