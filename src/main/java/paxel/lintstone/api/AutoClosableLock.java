package paxel.lintstone.api;

import java.util.concurrent.locks.ReentrantLock;

/**
 * A wrapper for {@link ReentrantLock} that implements {@link AutoCloseable}.
 * This allows using the lock in a try-with-resources block.
 */
public class AutoClosableLock implements AutoCloseable {

    private final ReentrantLock lock;

    /**
     * Creates a new AutoClosableLock and immediately locks the given lock.
     *
     * @param lock the lock to wrap and acquire.
     */
    public AutoClosableLock(ReentrantLock lock) {
        this.lock = lock;
        lock.lock();
    }

    @Override
    public void close()  {
        lock.unlock();
    }
}
