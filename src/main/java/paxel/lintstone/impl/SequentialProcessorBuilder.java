package paxel.lintstone.impl;

import paxel.lintstone.api.ErrorHandler;

import java.util.concurrent.ExecutorService;

/**
 * The SequentialProcessorBuilder class is responsible for constructing a SequentialProcessor object with the specified ExecutorService and ErrorHandler.
 */
public class SequentialProcessorBuilder {
    private final ExecutorService executorService;
    private ErrorHandler errorHandler = err -> true;

    /**
     * Constructs a SequentialProcessorBuilder object with the specified ExecutorService.
     *
     * @param executorService the ExecutorService to be used by the SequentialProcessor
     */
    public SequentialProcessorBuilder(ExecutorService executorService) {

        this.executorService = executorService;
    }

    /**
     * Sets the error handler for the SequentialProcessor.
     *
     * @param errorHandler The error handler to be set.
     */
    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    /**
     * Builds and returns a SequentialProcessor object using the specified error handler and executor service.
     *
     * @return The created SequentialProcessor object.
     */
    public SequentialProcessor build() {
        SequentialProcessorImpl sequentialProcessor = new SequentialProcessorImpl(errorHandler);
        executorService.submit(sequentialProcessor.getRunnable());
        return sequentialProcessor;
    }
}
