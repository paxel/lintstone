package paxel.lintstone.impl;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleSchedulerTest {

    @Test
    void testOrderAndWhereIsTheTwo() throws InterruptedException {
        SimpleScheduler scheduler = new SimpleScheduler();
        new Thread(scheduler::run).start();
        LinkedBlockingDeque<String> order = new LinkedBlockingDeque<>();

        scheduler.runLater(() -> order.add("five"), Duration.ofMillis(700));
        scheduler.runLater(() -> order.add("one"), Duration.ofMillis(100));
        scheduler.runLater(() -> order.add("4"), Duration.ofMillis(400));
        scheduler.runLater(() -> order.add("3"), Duration.ofMillis(300));
        CountDownLatch latch = new CountDownLatch(1);
        scheduler.runLater(() -> latch.countDown(), Duration.ofSeconds(1));
        latch.await();
        assertThat(order).containsExactly("one", "3", "4", "five");
    }

    @Test
    void testShutdownIdle() throws InterruptedException {
        SimpleScheduler scheduler = new SimpleScheduler();
        Thread thread = new Thread(scheduler);
        thread.start();
        Thread.sleep(200);
        long startTime = System.currentTimeMillis();
        scheduler.shutDown();
        thread.join(1000);
        assertThat(thread.isAlive()).isFalse();
        assertThat(System.currentTimeMillis() - startTime).isLessThan(500L);
    }

    @Test
    void testSameTimeTasksAreNotDropped() throws InterruptedException {
        SimpleScheduler scheduler = new SimpleScheduler();
        Thread schedulerThread = new Thread(scheduler);
        schedulerThread.start();

        int numTasks = 100;
        CountDownLatch latch = new CountDownLatch(numTasks);
        ConcurrentLinkedQueue<Integer> results = new ConcurrentLinkedQueue<>();

        Duration delay = Duration.ofMillis(100);
        for (int i = 0; i < numTasks; i++) {
            final int taskNum = i;
            scheduler.runLater(() -> {
                results.add(taskNum);
                latch.countDown();
            }, delay);
        }

        boolean finished = latch.await(2, TimeUnit.SECONDS);
        scheduler.shutDown();
        schedulerThread.join(1000);

        assertThat(results).hasSize(numTasks);
        assertThat(finished).isTrue();
    }

    @Test
    void testRunLaterAfterShutdown() {
        SimpleScheduler scheduler = new SimpleScheduler();
        scheduler.shutDown();
        
        Assertions.assertThatThrownBy(() -> scheduler.runLater(() -> {}, Duration.ofMillis(100)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Scheduler is shut down");
    }

    @Test
    void testShortDelayAccuracy() throws InterruptedException {
        SimpleScheduler scheduler = new SimpleScheduler();
        Thread thread = new Thread(scheduler);
        thread.start();

        CountDownLatch latch = new CountDownLatch(1);
        long startTime = System.nanoTime();

        // Schedule for 50ms
        scheduler.runLater(latch::countDown, Duration.ofMillis(50));

        boolean completed = latch.await(1, TimeUnit.SECONDS);
        long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

        scheduler.shutDown();
        thread.join(1000);

        assertThat(completed).isTrue();
        // Accuracy should be much better than 100ms now.
        assertThat(durationMillis).as("Duration should be close to 50ms, but was " + durationMillis)
                .isLessThan(150); // 150 is a safe margin for CI/test environments
    }
}