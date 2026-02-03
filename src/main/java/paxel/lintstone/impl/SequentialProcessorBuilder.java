package paxel.lintstone.impl;

import paxel.lintstone.api.ErrorHandler;
import paxel.lintstone.api.ErrorHandlerDecision;

import java.util.concurrent.ExecutorService;

/**
 * Builder for {@link SequentialProcessor}.
 */
public class SequentialProcessorBuilder {
    private final ExecutorService executorService;
    private ErrorHandler errorHandler = err -> ErrorHandlerDecision.CONTINUE;

    /**
     * Creates a new builder with the given executor service.
     *
     * @param executorService the executor service to use.
     */
    public SequentialProcessorBuilder(ExecutorService executorService) {

        this.executorService = executorService;
    }

    /**
     * Sets the error handler for the processor.
     *
     * @param errorHandler the error handler.
     */
    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    /**
     * Builds and starts the {@link SequentialProcessor}.
     *
     * @return the sequential processor.
     */
    public SequentialProcessor build() {
        SequentialProcessorImpl sequentialProcessor = new SequentialProcessorImpl(errorHandler);
        executorService.submit(sequentialProcessor.getRunnable());
        return sequentialProcessor;
    }
}
