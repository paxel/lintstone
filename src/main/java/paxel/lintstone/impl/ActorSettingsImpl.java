package paxel.lintstone.impl;

import paxel.bulkexecutor.ErrorHandler;
import paxel.lintstone.api.ActorSettings;

public class ActorSettingsImpl implements ActorSettings {
    private final int batch;
    private final ErrorHandler errorHandler;

    public ActorSettingsImpl(int batch, ErrorHandler errorHandler) {
        this.batch = batch;
        this.errorHandler = errorHandler;
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
