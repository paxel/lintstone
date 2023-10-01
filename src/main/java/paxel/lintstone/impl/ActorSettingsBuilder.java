package paxel.lintstone.impl;

import paxel.lintstone.api.ActorSettings;
import paxel.lintstone.api.ErrorHandler;

public class ActorSettingsBuilder {
    private ErrorHandler errorHandler = x -> true;

    public ActorSettingsBuilder setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
        return this;
    }


    public ActorSettings build() {
        return new ActorSettingsImpl( errorHandler);
    }

    public ErrorHandler getErrorHandler() {
        return this.errorHandler;
    }
}
