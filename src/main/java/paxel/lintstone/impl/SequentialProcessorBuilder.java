package paxel.lintstone.impl;

import paxel.lintstone.api.ErrorHandler;
import paxel.lintstone.api.ErrorHandlerDecision;

import java.util.concurrent.ExecutorService;

public class SequentialProcessorBuilder {
    private final ExecutorService executorService;
    private ErrorHandler errorHandler = err -> ErrorHandlerDecision.CONTINUE;

    public SequentialProcessorBuilder(ExecutorService executorService) {

        this.executorService = executorService;
    }

    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    public SequentialProcessor build() {
        SequentialProcessorImpl sequentialProcessor = new SequentialProcessorImpl(errorHandler);
        executorService.submit(sequentialProcessor.getRunnable());
        return sequentialProcessor;
    }
}
