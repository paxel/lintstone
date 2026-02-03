package paxel.lintstone.impl;

import org.junit.jupiter.api.Test;
import paxel.lintstone.api.ErrorHandlerDecision;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SequentialProcessorImplTest {

    @Test
    public void testSequentialExecution() throws InterruptedException {
        SequentialProcessorImpl processor = new SequentialProcessorImpl(e -> ErrorHandlerDecision.CONTINUE);
        List<Integer> results = Collections.synchronizedList(new ArrayList<>());
        int count = 1000;
        CountDownLatch latch = new CountDownLatch(count);

        for (int i = 0; i < count; i++) {
            final int val = i;
            processor.add(() -> {
                results.add(val);
                latch.countDown();
            });
        }

        Thread t = new Thread(processor.getRunnable());
        t.start();

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        processor.unregisterGracefully();
        t.join(1000);

        assertThat(results).hasSize(count);
        for (int i = 0; i < count; i++) {
            assertThat(results.get(i)).isEqualTo(i);
        }
    }

    @Test
    public void testNoConcurrentExecution() throws InterruptedException {
        SequentialProcessorImpl processor = new SequentialProcessorImpl(e -> ErrorHandlerDecision.CONTINUE);
        AtomicInteger activeThreads = new AtomicInteger(0);
        AtomicInteger maxActiveThreads = new AtomicInteger(0);
        int count = 100;
        CountDownLatch latch = new CountDownLatch(count);

        for (int i = 0; i < count; i++) {
            processor.add(() -> {
                int current = activeThreads.incrementAndGet();
                synchronized (maxActiveThreads) {
                    if (current > maxActiveThreads.get()) {
                        maxActiveThreads.set(current);
                    }
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                activeThreads.decrementAndGet();
                latch.countDown();
            });
        }

        Thread t = new Thread(processor.getRunnable());
        t.start();

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        processor.unregisterGracefully();
        t.join(1000);

        assertThat(maxActiveThreads.get()).isEqualTo(1);
    }

    @Test
    public void testBackPressure() throws InterruptedException {
        SequentialProcessorImpl processor = new SequentialProcessorImpl(e -> ErrorHandlerDecision.CONTINUE);
        int threshold = 10;
        CountDownLatch startedLatch = new CountDownLatch(1);
        CountDownLatch blockLatch = new CountDownLatch(1);

        processor.add(() -> {
            startedLatch.countDown();
            try {
                blockLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread processorThread = new Thread(processor.getRunnable());
        processorThread.start();

        assertTrue(startedLatch.await(1, TimeUnit.SECONDS));

        // Fill the queue up to threshold
        for (int i = 0; i < threshold; i++) {
            processor.add(() -> {});
        }

        // Next addWithBackPressure should block
        CountDownLatch addBlockedLatch = new CountDownLatch(1);
        Thread adderThread = new Thread(() -> {
            try {
                processor.addWithBackPressure(() -> {}, threshold);
                addBlockedLatch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        adderThread.start();

        // Should be blocked
        assertThat(addBlockedLatch.await(500, TimeUnit.MILLISECONDS)).isFalse();

        // Unblock first task
        blockLatch.countDown();

        // Adder should now finish
        assertTrue(addBlockedLatch.await(1, TimeUnit.SECONDS));

        processor.unregisterGracefully();
        processorThread.join(1000);
    }

    @Test
    public void testShutdownNow() throws InterruptedException {
        SequentialProcessorImpl processor = new SequentialProcessorImpl(e -> ErrorHandlerDecision.CONTINUE);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch block = new CountDownLatch(1);

        processor.add(() -> {
            started.countDown();
            try {
                block.await();
            } catch (InterruptedException e) {
                // Ignore
            }
        });

        for (int i = 0; i < 10; i++) {
            processor.add(() -> {});
        }

        Thread t = new Thread(processor.getRunnable());
        t.start();

        assertTrue(started.await(1, TimeUnit.SECONDS));
        processor.shutdown(true);
        block.countDown();

        t.join(1000);
        assertThat(processor.size()).isZero();
    }
}
