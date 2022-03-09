package paxel.lintstone.api;

import paxel.bulkexecutor.ErrorHandler;
import paxel.lintstone.impl.ActorSettingsBuilder;

/**
 * The actor settings for the creation of configured actors.
 */
public interface ActorSettings {
    /**
     * Defines if the Actor receives messages from multiple sources.
     *
     * @return true if multiple sources send messages to the actor.
     */
    boolean isMulti();

    /**
     * The number of messages that should be processed by the actor in one batch.
     *
     * @return the batch size.
     */
    int getBatch();

    /**
     * The limit of the input queue of the actor.
     *
     * @return the limit.
     */
    int getLimit();

    /**
     * The handler for uncaught exceptions in the actor.
     *
     * @return the error handler.
     */
    ErrorHandler getErrorHandler();

    static ActorSettingsBuilder create() {
        return new ActorSettingsBuilder();
    }
}
