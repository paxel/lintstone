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
}