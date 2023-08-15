package paxel.lintstone.api;

import paxel.bulkexecutor.ErrorHandler;

/**
 * The actor settings for the creation of configured actors.
 */
public interface ActorSettings {

    /**
     * Non-blocking, unlimited and one by one processing Settings. Save but not optimal.
     */
    ActorSettings DEFAULT = ActorSettings.create().build();

    /**
     * Defines if the Actor receives messages from multiple sources.
     *
     * @return true if multiple sources send messages to the actor.
     */
    boolean isMulti();

    /**
     * Defines if the send message to the actor should block until the limited queue has space for another message or if the message is ignored (and returned false.
     *
     * @return true if the send message should block until the message can be enqueued.
     */
    boolean isBlocking();

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

    /**
     * Create a builder to build an implementation of the Settings.
     *
     * @return a builder.
     */
    static ActorSettingsBuilder create() {
        return new ActorSettingsBuilder();
    }
}
