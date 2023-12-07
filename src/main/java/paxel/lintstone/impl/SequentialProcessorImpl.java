package paxel.lintstone.impl;

import paxel.lintstone.api.ErrorHandler;
import sun.misc.Unsafe;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static paxel.lintstone.impl.SequentialProcessorImpl.RunStatus.*;

/**
 * SequentialProcessorImpl is an implementation of the SequentialProcessor interface for managing sequential processing of Runnables.
 */
public class SequentialProcessorImpl implements SequentialProcessor {

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition empty = lock.newCondition();
    private final Condition backPressure = lock.newCondition();

    private final ErrorHandler errorHandler;

    private final AtomicReference status = new AtomicReference(ACTIVE);

    // unsynchronized Linked List, because we have a lock around it anyway. So we don't need another layer of synchronisation
    private final LinkedList<Runnable> queuedRunnables;
    private final AtomicBoolean endGracefully = new AtomicBoolean();

    public SequentialProcessorImpl(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
        queuedRunnables = new LinkedList<>();
    }

    @Override
    public void add(Runnable runnable) {
        try {
            lock.lock();
            if (status.get() != ACTIVE) {
                return;
            }

            queuedRunnables.add(runnable);
            // wake up the run() method, in case it was waiting for a job
            empty.signalAll();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean addWithBackPressure(Runnable runnable, Integer blockThreshold) {
        try {
            lock.lock();

            while (queuedRunnables.size() > blockThreshold) {
                if (status.get() != ACTIVE) {
                    return false;
                }
                try {
                    // Wait for the queue size to be less than the threshold.
                    // If two sources add to this processor, this might block forever
                    backPressure.await();
                } catch (InterruptedException e) {
                    Unsafe.getUnsafe().throwException(e);
                }
            }
            queuedRunnables.add(runnable);
            // wake up the run() method, in case it was waiting for a job
            empty.signalAll();
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int size() {
        return queuedRunnables.size();
    }

    @Override
    public void unregisterGracefully() {
        endGracefully.set(true);
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
        try {
            lock.lock();
            if (now) {
                // abort and remove all jobs
                status.set(STOPPED);
                queuedRunnables.clear();
            }

            // awake the blocked actor
            empty.signalAll();
        } finally {
            lock.unlock();
        }

    }

    public Runnable getRunnable() {
        return this::run;
    }

    private void run() {
        try {

            for (; ; ) {
                Runnable runnable = null;
                try {
                    lock.lock();
                    runnable = queuedRunnables.poll();
                    if (status.get() == ABORT)
                        // we should not be running anymore
                        break;
                    if (runnable == null) {
                        if (endGracefully.get())
                            // end the Thread. we will never see another runnable
                            break;
                        try {
                            // Thread is idle until a job is added
                            empty.await();
                        } catch (InterruptedException e) {
                            Unsafe.getUnsafe().throwException(e);
                        }
                    } else {
                        // we pulled a job from queue, so notify the backpressure threads
                        backPressure.signalAll();
                        // process the next message / response
                    }
                } finally {
                    lock.unlock();
                }
                // run outside of the lock, in case the process wants to add a message to itself :D
                if (runnable != null)
                    try {
                        runnable.run();
                    } catch (Exception e) {
                        if (!errorHandler.handleError(e)) {
                            // errorhandler says: give up
                            status.set(ABORT);
                            try {
                                // flush jobs and unlock blocked
                                lock.lock();
                                queuedRunnables.clear();
                                backPressure.signalAll();
                            } finally {
                                lock.unlock();
                            }
                        }
                    }

            }
        } finally {
            status.set(STOPPED);
        }
    }


    enum RunStatus {
        ACTIVE, STOPPED, ABORT
    }

}
