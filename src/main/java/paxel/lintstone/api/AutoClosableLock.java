package paxel.lintstone.api;

import java.util.concurrent.locks.ReentrantLock;

public class AutoClosableLock implements AutoCloseable {

    private final ReentrantLock lock;

    public AutoClosableLock(ReentrantLock lock) {
        this.lock = lock;
        lock.lock();
    }

    @Override
    public void close()  {
        lock.unlock();
    }
}
