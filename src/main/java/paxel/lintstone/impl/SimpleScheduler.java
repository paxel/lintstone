package paxel.lintstone.impl;

import paxel.lintstone.api.Scheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Simple implementation of a Timer without Synchronize keyword usage
 */
public class SimpleScheduler implements Scheduler, Runnable {

    private final ConcurrentSkipListSet<ScheduledRunnable> jobs = new ConcurrentSkipListSet<>();

    private final AtomicBoolean stop = new AtomicBoolean(false);
    ReentrantLock lock = new ReentrantLock();
    Condition newJob = lock.newCondition();

    /**
     * Creates a new simple scheduler.
     */
    public SimpleScheduler() {
    }

    @Override
    public void runLater(Runnable runnable, Duration duration) {
        lock.lock();
        try {
            ScheduledRunnable scheduledRunnable = new ScheduledRunnable(Instant.now().plus(duration), runnable);
            jobs.add(scheduledRunnable);
            newJob.signalAll();
        } finally {
            lock.unlock();
        }
    }

    private TimerTask wrapRunnable(Runnable runnable) {
        return new TimerTask() {
            @Override
            public void run() {
                runnable.run();
            }
        };
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

    private record ScheduledRunnable(Instant start, Runnable runnable) implements Comparable<ScheduledRunnable> {
        @Override
        public int compareTo(ScheduledRunnable o) {
            return this.start.compareTo(o.start);
        }
    }
}
