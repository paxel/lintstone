package paxel.lintstone.impl;

import paxel.lintstone.api.Scheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Simple implementation of a Timer without Synchronize keyword usage
 */
public class SimpleScheduler implements Scheduler, Runnable {

    private final ConcurrentSkipListSet<ScheduledRunnable> jobs = new ConcurrentSkipListSet<>();
    private final AtomicLong sequencer = new AtomicLong(0);

    private final AtomicBoolean stop = new AtomicBoolean(false);
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition newJob = lock.newCondition();

    /**
     * Creates a new simple scheduler.
     */
    public SimpleScheduler() {
    }

    @Override
    public void runLater(Runnable runnable, Duration duration) {
        lock.lock();
        try {
            if (stop.get()) {
                throw new IllegalStateException("Scheduler is shut down");
            }
            ScheduledRunnable scheduledRunnable = new ScheduledRunnable(Instant.now().plus(duration), sequencer.getAndIncrement(), runnable);
            jobs.add(scheduledRunnable);
            newJob.signalAll();
        } finally {
            lock.unlock();
        }
    }


    @Override
    public void shutDown() {
        lock.lock();
        try {
            this.stop.set(true);
            newJob.signalAll();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void run() {
        try {

            while (!stop.get()) {
                lock.lock();
                try {
                    if (jobs.isEmpty()) {
                        newJob.await();
                    }
                    if (!jobs.isEmpty()) {
                        if (jobs.getFirst().start.isAfter(Instant.now())) {
                            // next job is in the future so we need to wait until it is ready, or a new one arrives
                            newJob.await(Math.max(100L, Duration.between(Instant.now(), jobs.getFirst().start()).toMillis() + 10), TimeUnit.MILLISECONDS);
                        } else {
                            ScheduledRunnable scheduledRunnable = jobs.pollFirst();
                            scheduledRunnable.runnable().run();
                        }
                    }
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private record ScheduledRunnable(Instant start, long sequenceNumber, Runnable runnable) implements Comparable<ScheduledRunnable> {
        @Override
        public int compareTo(ScheduledRunnable o) {
            int res = this.start.compareTo(o.start);
            if (res == 0) {
                return Long.compare(this.sequenceNumber, o.sequenceNumber);
            }
            return res;
        }
    }
}
