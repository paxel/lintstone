package paxel.lintstone.impl;

import paxel.lintstone.api.ErrorHandler;
import paxel.lintstone.api.ErrorHandlerDecision;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static paxel.lintstone.impl.SequentialProcessorImpl.RunStatus.*;

/**
 * Implementation of {@link SequentialProcessor}.
 */
public class SequentialProcessorImpl implements SequentialProcessor {

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition empty = lock.newCondition();
    private final Semaphore backPressureSemaphore = new Semaphore(0);

    private final ErrorHandler errorHandler;

    private final AtomicReference<RunStatus> status = new AtomicReference<>(ACTIVE);

    private final ConcurrentLinkedQueue<Runnable> queuedRunnables = new ConcurrentLinkedQueue<>();
    private final AtomicInteger queueSize = new AtomicInteger(0);
    private final AtomicBoolean endGracefully = new AtomicBoolean();

    /**
     * Creates a new sequential processor implementation with the given error handler.
     *
     * @param errorHandler the error handler.
     */
    public SequentialProcessorImpl(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    @Override
    public void add(Runnable runnable) {
        if (status.get() != ACTIVE || endGracefully.get()) {
            return;
        }

        queuedRunnables.add(runnable);
        queueSize.incrementAndGet();
        try {
            lock.lock();
            // wake up the run() method, in case it was waiting for a job
            empty.signalAll();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean addWithBackPressure(Runnable runnable, int blockThreshold) throws InterruptedException {
        if (blockThreshold <= 0) {
            throw new IllegalArgumentException("blockThreshold must be greater than 0");
        }
        while (queueSize.get() >= blockThreshold) {
            if (status.get() != ACTIVE || endGracefully.get()) {
                return false;
            }
            backPressureSemaphore.acquire();
        }

        if (status.get() != ACTIVE || endGracefully.get()) {
            return false;
        }

        queuedRunnables.add(runnable);
        queueSize.incrementAndGet();
        try {
            lock.lock();
            // wake up the run() method, in case it was waiting for a job
            empty.signalAll();
        } finally {
            lock.unlock();
        }
        return true;
    }

    @Override
    public int size() {
        return queueSize.get();
    }

    @Override
    public void unregisterGracefully() {
        endGracefully.set(true);
        // Wake up potentially blocked threads
        backPressureSemaphore.release(65536);
        try {
            lock.lock();
            // awake the blocked actor
            empty.signalAll();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void shutdown(boolean now) {
        endGracefully.set(true);
        if (now) {
            // abort and remove all jobs
            status.set(STOPPED);
            queuedRunnables.clear();
            queueSize.set(0);
        }
        // Wake up potentially blocked threads. 
        // Once endGracefully is true, no new threads will block on this semaphore.
        // We release a large number to ensure all current and racing waiters are woken up.
        backPressureSemaphore.release(65536);
        try {
            lock.lock();
            // awake the blocked actor
            empty.signalAll();
        } finally {
            lock.unlock();
        }

    }

    /**
     * Gets the runnable that performs the processing.
     *
     * @return the processing runnable.
     */
    public Runnable getRunnable() {
        return this::run;
    }

    private void run() {
        try {
            runMessages();
        } finally {
            status.set(STOPPED);
        }
    }

    private void runMessages() {
        for (; ; ) {
            // Poll blocks until a message is available. If null is returned we should stop
            Runnable runnable = poll();
            if (runnable == null) {
                break;
            }
            runNextMessage(runnable);
        }
    }

    private Runnable poll() {
        for (; ; ) {
            Runnable runnable = queuedRunnables.poll();
            if (runnable != null) {
                queueSize.decrementAndGet();
                backPressureSemaphore.release();
                return runnable;
            }

            if (!checkWaiting()) {
                // There is no Runnable and there will never be one again.
                return null;
            }
        }
    }

    private boolean checkWaiting() {
        if (status.get() == ABORT) {
            return false;
        }
        if (endGracefully.get() && queuedRunnables.isEmpty()) {
            return false;
        }

        try {
            lock.lock();
            if (queuedRunnables.isEmpty()) {
                if (endGracefully.get()) {
                    return false;
                }
                // Set this Thread to inactive until a message is received
                empty.await();
            }
            return true;
        } catch (InterruptedException e) {
            // Restore interrupted status and end this Thread
            Thread.currentThread().interrupt();
            return false;
        } finally {
            lock.unlock();
        }
    }

    private void runNextMessage(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            if (errorHandler.handleError(e) != ErrorHandlerDecision.CONTINUE) {
                // errorhandler says: give up
                status.set(ABORT);
                queuedRunnables.clear();
                queueSize.set(0);
                backPressureSemaphore.release(65536);
            }
        }
    }


    enum RunStatus {
        ACTIVE, STOPPED, ABORT
    }
}
