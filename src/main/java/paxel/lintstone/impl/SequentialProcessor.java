package paxel.lintstone.impl;

public interface SequentialProcessor {
    void add(Runnable runnable);

    boolean addWithBackPressure(Runnable runnable, int blockThreshold) throws InterruptedException;

    int size();

    void unregisterGracefully();

    void shutdown(boolean now);
}
