package paxel.lintstone.api;

import java.time.Duration;

/**
 * Interface for scheduling tasks.
 */
public interface Scheduler {

    /**
     * Run the runnable after the duration has passed.
     *
     * @param runnable The runnable to execute
     * @param duration The duration to wait
     */
    void runLater(Runnable runnable, Duration duration);

    /**
     * Stops the scheduler. Currently running runnables are finished, but no other Runnables will be executed.
     */
    void shutDown();
}
