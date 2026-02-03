package paxel.lintstone.impl;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;

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

}