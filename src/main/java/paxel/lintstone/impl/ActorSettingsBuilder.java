package paxel.lintstone.impl;

import paxel.lintstone.api.ActorSettings;
import paxel.lintstone.api.ErrorHandler;
import paxel.lintstone.api.ErrorHandlerDecision;

/**
 * Builder for {@link ActorSettings}.
 */
public class ActorSettingsBuilder {
    private ErrorHandler errorHandler = x -> ErrorHandlerDecision.CONTINUE;
    private int queueLimit;

    /**
     * Creates a new actor settings builder.
     */
    public ActorSettingsBuilder() {
    }

    /**
     * Sets the error handler for the actor.
     *
     * @param errorHandler the error handler.
     * @return this builder.
     */
    public ActorSettingsBuilder setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
        return this;
    }

    /**
     * Sets the maximum number of messages that can be queued for this actor.
     *
     * @param queueLimit the queue limit.
     * @return this builder.
     */
    public ActorSettingsBuilder setQueueLimit(int queueLimit) {
        this.queueLimit = queueLimit;
        return this;
    }


    /**
     * Builds the {@link ActorSettings} instance.
     *
     * @return the actor settings.
     */
    public ActorSettings build() {
        return new ActorSettingsImpl(errorHandler, queueLimit);
    }

    /**
     * Gets the current error handler.
     *
     * @return the error handler.
     */
    public ErrorHandler getErrorHandler() {
        return this.errorHandler;
    }
}
