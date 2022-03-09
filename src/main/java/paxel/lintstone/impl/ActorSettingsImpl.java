package paxel.lintstone.impl;

import paxel.bulkexecutor.ErrorHandler;
import paxel.lintstone.api.ActorSettings;

public class ActorSettingsImpl implements ActorSettings {
    private int limit;
    private boolean multi;
    private int batch;
    private ErrorHandler errorHandler;

    public ActorSettingsImpl(int limit, boolean multi, int batch, ErrorHandler errorHandler) {
        this.limit = limit;
        this.multi = multi;
        this.batch = batch;
        this.errorHandler = errorHandler;
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
}
