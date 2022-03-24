package paxel.lintstone.impl;

import paxel.bulkexecutor.ErrorHandler;
import paxel.lintstone.api.ActorSettings;

public class ActorSettingsImpl implements ActorSettings {
    private final int limit;
    private final boolean multi;
    private final boolean blocking;
    private final int batch;
    private final ErrorHandler errorHandler;

    public ActorSettingsImpl(int limit, boolean multi, int batch, ErrorHandler errorHandler, boolean blocking) {
        this.limit = limit;
        this.multi = multi;
        this.batch = batch;
        this.errorHandler = errorHandler;
        this.blocking = blocking;
    }

    @Override
    public int getLimit() {
        return limit;
    }

    @Override
    public boolean isMulti() {
        return multi;
    }

    @Override
    public int getBatch() {
        return batch;
    }

    @Override
    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    @Override
    public boolean isBlocking() {
        return blocking;
    }
}
