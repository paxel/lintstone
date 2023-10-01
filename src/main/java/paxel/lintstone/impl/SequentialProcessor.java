package paxel.lintstone.impl;

public interface SequentialProcessor {
    void add(Runnable runnable);

    boolean addWithBackPressure(Runnable runnable, Integer blockThreshold);

    int size();

    void unregisterGracefully();

    void shutdown(boolean now);
}
