package paxel.lintstone.impl;

import org.junit.jupiter.api.Test;
import paxel.lintstone.api.ActorSettings;
import paxel.lintstone.api.LintStoneActor;
import paxel.lintstone.api.LintStoneActorAccessor;
import paxel.lintstone.api.LintStoneSystem;
import paxel.lintstone.api.LintStoneSystemFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class AccessorVisibilityTest {

    @Test
    void testAccessorConcurrentAccess() throws InterruptedException {
        LintStoneSystem system = LintStoneSystemFactory.create();
        String actorName = "visibility-actor";
        
        AtomicInteger counter = new AtomicInteger(0);
        LintStoneActor actor = mec -> mec.inCase(String.class, (msg, ctx) -> {
            counter.incrementAndGet();
        });

        system.registerActor(actorName, () -> actor, ActorSettings.DEFAULT);
        LintStoneActorAccessor accessor = system.getActor(actorName);

        int numThreads = 10;
        int messagesPerThread = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < messagesPerThread; j++) {
                        accessor.tell("msg");
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        
        // Wait for messages to be processed
        long start = System.currentTimeMillis();
        while (counter.get() < numThreads * messagesPerThread && System.currentTimeMillis() - start < 5000) {
            Thread.sleep(10);
        }

        assertThat(counter.get()).isEqualTo(numThreads * messagesPerThread);
        
        system.shutDown();
        executor.shutdown();
    }
}
