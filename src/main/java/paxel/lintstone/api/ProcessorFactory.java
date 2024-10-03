package paxel.lintstone.api;

import paxel.lintstone.impl.SequentialProcessorBuilder;

import java.util.List;
import java.util.concurrent.TimeUnit;

public interface ProcessorFactory {
    SequentialProcessorBuilder create();

    void shutdown();

    List<Runnable> shutdownNow();

    boolean isShutdown();

    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;
}
