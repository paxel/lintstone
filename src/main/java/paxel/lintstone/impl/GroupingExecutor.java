package paxel.lintstone.impl;

import paxel.lintstone.api.ProcessorFactory;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * A {@link ProcessorFactory} implementation that uses a virtual thread per task executor.
 */
public class GroupingExecutor implements ProcessorFactory {
    private final ExecutorService executorService;


    /**
     * Creates a new GroupingExecutor with a virtual thread per task executor.
     */
    public GroupingExecutor() {
        // this implementation is completely for virtual Threads
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
    }

    @Override
    public SequentialProcessorBuilder create() {
        // the Builder will submit the runnable to the service when the Processor is build.
        return new SequentialProcessorBuilder(executorService);
    }

    @Override
    public void shutdown() {
        executorService.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return executorService.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return executorService.isShutdown();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executorService.awaitTermination(timeout, unit);
    }

    @Override
    public String toString() {
        return "GroupingExecutor{" +
                "executorService=" + executorService +
                '}';
    }
}
