package paxel.lintstone.impl;

import paxel.lintstone.api.ErrorHandler;
import paxel.lintstone.api.ErrorHandlerDecision;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static paxel.lintstone.impl.SequentialProcessorImpl.RunStatus.*;

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
    public boolean addWithBackPressure(Runnable runnable, int blockThreshold) throws InterruptedException {
        try {
            lock.lock();

            while (queuedRunnables.size() > blockThreshold) {
                if (status.get() != ACTIVE) {
                    return false;
                }
                    // Wait for the queue size to be less than the threshold.
                    // If two sources add to this processor, this might block forever
                backPressure.await();
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
        try {
            lock.lock();
            for (; ; ) {
                Runnable runnable = queuedRunnables.poll();
                if (!checkRunnable(runnable)) {
                    // There is no Runnable and there will never be one again.
                    return null;
                }
                //It is valid. So if it is not null we use it. Or retry.
                if (runnable != null) {
                    return runnable;
                }
            }
        } finally {
            lock.unlock();
        }

    }

    private boolean checkRunnable(Runnable runnable) {
        if (status.get() == ABORT)
            // we should not be running anymore
            return false;
        if (runnable == null) {
            if (endGracefully.get()) {
                // end the Thread. we will never see another runnable
                return false;
            }
            try {
                // Set this Thread to inactive until a message is received
                empty.await();
                return true;
            } catch (InterruptedException e) {
                // End this Thread
                return false;
            }
        } else {
            // we pulled a job from queue, so notify the backpressure threads
            backPressure.signalAll();
            // process the next message / response
            return true;
        }
    }

    private void runNextMessage(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            if (errorHandler.handleError(e) != ErrorHandlerDecision.CONTINUE) {
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


    enum RunStatus {
        ACTIVE, STOPPED, ABORT
    }
}
