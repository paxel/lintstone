package paxel.lintstone.impl;

import paxel.lintstone.impl.SequentialProcessorBuilder;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class GroupingExecutor {
    private final ExecutorService executorService;


    public GroupingExecutor() {
        // this implementation is completely for virtual Threads
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
    }

    public SequentialProcessorBuilder create() {
        // the Builder will submit the runnable to the service when the Processor is build.
        return new SequentialProcessorBuilder(executorService);
    }

    public void shutdown() {
        executorService.shutdown();
    }

    public List<Runnable> shutdownNow() {
        return executorService.shutdownNow();
    }

    public boolean isShutdown() {
        return executorService.isShutdown();
    }

    public boolean isTerminated() {
        return executorService.isTerminated();
    }

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
