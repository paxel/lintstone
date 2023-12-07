package paxel.lintstone.impl;

import paxel.lintstone.api.ActorSettings;
import paxel.lintstone.api.ErrorHandler;

/**
 * A builder class for creating instances of {@link ActorSettings}.
 */
public class ActorSettingsBuilder {
    private ErrorHandler errorHandler = x -> true;

    /**
     * Sets the error handler for the actor settings.
     *
     * @param errorHandler the error handler to set
     * @return the updated instance of ActorSettingsBuilder
     */
    public ActorSettingsBuilder setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
        return this;
    }

    /**
     * Builds an instance of {@link ActorSettings} with the specified configuration.
     *
     * @return the built instance of ActorSettings
     */
    public ActorSettings build() {
        return new ActorSettingsImpl( errorHandler);
    }

    /**
     * Retrieves the error handler for the actor settings.
     *
     * @return the error handler.
     */
    public ErrorHandler getErrorHandler() {
        return this.errorHandler;
    }
}
