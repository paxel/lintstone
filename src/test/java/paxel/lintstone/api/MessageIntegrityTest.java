package paxel.lintstone.api;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MessageIntegrityTest {

    @Test
    public void testNoMessageLossUnderContention() throws InterruptedException, ExecutionException {
        LintStoneSystem system = LintStoneSystemFactory.create();
        int threadCount = 8;
        int messagesPerThread = 10000;
        int totalExpectedMessages = threadCount * messagesPerThread;

        LintStoneActorAccessor actor = system.registerActor("counter", CounterActor::new, ActorSettings.DEFAULT);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < messagesPerThread; j++) {
                        actor.tell(1);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(finishLatch.await(30, TimeUnit.SECONDS));
        executor.shutdown();

        // Use ask to get the final count and ensure all messages were processed
        CompletableFuture<Integer> countFuture = actor.ask("GET");
        Integer finalCount = countFuture.get();

        assertThat(finalCount, is(totalExpectedMessages));

        system.shutDownAndWait(Duration.ofSeconds(5));
    }

    @Test
    public void testPerSenderOrdering() throws InterruptedException, ExecutionException {
        LintStoneSystem system = LintStoneSystemFactory.create();
        int threadCount = 4;
        int messagesPerThread = 1000;

        LintStoneActorAccessor actor = system.registerActor("orderVerifier", OrderVerifierActor::new, ActorSettings.DEFAULT);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            futures.add(executor.submit(() -> {
                for (int j = 0; j < messagesPerThread; j++) {
                    actor.tell(new OrderedMessage(threadId, j));
                }
            }));
        }

        for (Future<?> future : futures) {
            future.get();
        }
        executor.shutdown();

        CompletableFuture<Boolean> resultFuture = actor.ask("VERIFY");
        assertTrue(resultFuture.get(), "Messages from at least one thread were out of order or missing");

        system.shutDownAndWait(Duration.ofSeconds(5));
    }

    private static class CounterActor implements LintStoneActor {
        private int count = 0;

        @Override
        public void newMessageEvent(LintStoneMessageEventContext mec) {
            mec.inCase(Integer.class, (i, ctx) -> {
                count += i;
            }).inCase(String.class, (s, ctx) -> {
                if ("GET".equals(s)) {
                    ctx.reply(count);
                }
            });
        }
    }

    private static class OrderedMessage {
        final int threadId;
        final int sequence;

        OrderedMessage(int threadId, int sequence) {
            this.threadId = threadId;
            this.sequence = sequence;
        }
    }

    private static class OrderVerifierActor implements LintStoneActor {
        private final int[] lastSequence = new int[10]; // assuming threadId < 10
        private boolean failed = false;

        public OrderVerifierActor() {
            for (int i = 0; i < lastSequence.length; i++) {
                lastSequence[i] = -1;
            }
        }

        @Override
        public void newMessageEvent(LintStoneMessageEventContext mec) {
            mec.inCase(OrderedMessage.class, (msg, ctx) -> {
                if (msg.sequence != lastSequence[msg.threadId] + 1) {
                    failed = true;
                    System.err.println("Order failure for thread " + msg.threadId + ": expected " + (lastSequence[msg.threadId] + 1) + " but got " + msg.sequence);
                }
                lastSequence[msg.threadId] = msg.sequence;
            }).inCase(String.class, (s, ctx) -> {
                if ("VERIFY".equals(s)) {
                    ctx.reply(!failed);
                }
            });
        }
    }
}
