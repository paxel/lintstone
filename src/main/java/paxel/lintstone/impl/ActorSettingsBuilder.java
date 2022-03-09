package paxel.lintstone.impl;

import paxel.bulkexecutor.ErrorHandler;
import paxel.lintstone.api.ActorSettings;

public class ActorSettingsBuilder {
    private int batch = 1;
    private int limit = 0;
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
        return new ActorSettingsImpl(limit,multi,batch,errorHandler);
    }
}
