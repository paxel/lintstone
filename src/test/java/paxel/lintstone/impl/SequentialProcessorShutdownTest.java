package paxel.lintstone.impl;

import org.junit.jupiter.api.Test;
import paxel.lintstone.api.ErrorHandlerDecision;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

public class SequentialProcessorShutdownTest {

    @Test
    void testNoProducerStarvationOnGracefulShutdown() throws InterruptedException {
        SequentialProcessorImpl processor = new SequentialProcessorImpl((err, desc, cause) -> ErrorHandlerDecision.CONTINUE);
        
        // Fill the processor to the threshold
        int threshold = 10;
        for (int i = 0; i < threshold; i++) {
            processor.add(() -> {});
        }
        // Force queueSize to be at threshold
        assertThat(processor.size()).isEqualTo(threshold);

        // Start a producer that will block
        AtomicBoolean producerCompleted = new AtomicBoolean(false);
        AtomicBoolean addResult = new AtomicBoolean(true);
        CountDownLatch producerStarted = new CountDownLatch(1);
        Thread producerThread = new Thread(() -> {
            try {
                producerStarted.countDown();
                boolean added = processor.addWithBackPressure(() -> {}, threshold);
                addResult.set(added);
                producerCompleted.set(true);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        producerThread.start();
        
        // Ensure producer is blocked
        producerStarted.await();
        Thread.sleep(200);
        assertThat(producerCompleted.get()).isFalse();

        // Now shutdown gracefully.
        processor.shutdown(false);
        
        // Producer should be woken up IMMEDIATELY
        producerThread.join(1000);
        assertThat(producerCompleted.get()).isTrue();
        // It should have returned false because of shutdown
        assertThat(addResult.get()).isFalse();
    }

    @Test
    void testNoProducerStarvationOnImmediateShutdown() throws InterruptedException {
        SequentialProcessorImpl processor = new SequentialProcessorImpl((err, desc, cause) -> ErrorHandlerDecision.CONTINUE);
        
        int threshold = 10;
        for (int i = 0; i < threshold; i++) {
            processor.add(() -> {});
        }

        CountDownLatch producerStarted = new CountDownLatch(1);
        AtomicBoolean producerCompleted = new AtomicBoolean(false);
        AtomicBoolean addResult = new AtomicBoolean(true);
        Thread producerThread = new Thread(() -> {
            try {
                producerStarted.countDown();
                boolean added = processor.addWithBackPressure(() -> {}, threshold);
                addResult.set(added);
                producerCompleted.set(true);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        producerThread.start();
        
        producerStarted.await();
        Thread.sleep(200);
        assertThat(producerCompleted.get()).isFalse();

        // Immediate shutdown
        processor.shutdown(true);
        
        producerThread.join(1000);
        assertThat(producerCompleted.get()).isTrue();
        assertThat(addResult.get()).isFalse();
    }

    @Test
    void testValidationOfBlockThreshold() {
        SequentialProcessorImpl processor = new SequentialProcessorImpl((err, desc, cause) -> ErrorHandlerDecision.CONTINUE);
        
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> processor.addWithBackPressure(() -> {}, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("blockThreshold must be greater than 0");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> processor.addWithBackPressure(() -> {}, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("blockThreshold must be greater than 0");
    }
}
