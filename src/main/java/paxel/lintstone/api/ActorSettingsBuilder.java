package paxel.lintstone.api;

import lombok.Getter;
import paxel.bulkexecutor.ErrorHandler;
import paxel.lintstone.impl.ActorSettingsImpl;

@Getter
public class ActorSettingsBuilder {
    private int batch = 1;
    private ErrorHandler errorHandler = x -> true;

    public ActorSettingsBuilder setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
        return this;
    }

    public ActorSettingsBuilder setBatch(int batch) {
        this.batch = batch;
        return this;
    }


    public ActorSettings build() {
        return new ActorSettingsImpl(batch, errorHandler);
    }
}
