package paxel.lintstone.api;

import paxel.bulkexecutor.ErrorHandler;
import paxel.lintstone.impl.ActorSettingsImpl;

public class ActorSettingsBuilder {
    private boolean blocking;
    private int batch = 1;
    private int limit;
    private boolean multi = true;
    private ErrorHandler errorHandler = x -> true;

    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    public ActorSettingsBuilder setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
        return this;
    }

    public int getBatch() {
        return batch;
    }

    public ActorSettingsBuilder setBatch(int batch) {
        this.batch = batch;
        return this;
    }

    public boolean isBlocking() {
        return blocking;
    }

    public ActorSettingsBuilder setBlocking(boolean blocking) {
        this.blocking = blocking;
        return this;
    }

    public long getLimit() {
        return limit;
    }

    public ActorSettingsBuilder setLimit(int limit) {
        this.limit = limit;
        return this;
    }

    public boolean isMulti() {
        return multi;
    }

    public ActorSettingsBuilder setMulti(boolean multi) {
        this.multi = multi;
        return this;
    }

    public ActorSettings build() {
        return new ActorSettingsImpl(limit,multi,batch,errorHandler, blocking);
    }
}
