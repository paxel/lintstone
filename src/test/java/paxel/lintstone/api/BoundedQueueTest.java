package paxel.lintstone.api;

import org.junit.jupiter.api.Test;
import paxel.lintstone.impl.SimpleScheduler;
import paxel.lintstone.impl.ActorSystem;
import paxel.lintstone.api.ActorSettings;
import paxel.lintstone.api.LintStoneActor;
import paxel.lintstone.api.LintStoneActorAccessor;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

public class BoundedQueueTest {

    @Test
    void testBoundedQueueForTell() throws InterruptedException {
        ActorSystem system = new ActorSystem();
        CountDownLatch blockActor = new CountDownLatch(1);
        int limit = 5;
        ActorSettings settings = ActorSettings.create().setQueueLimit(limit).build();
        
        LintStoneActorAccessor actor = system.registerActor("test", () -> mec -> {
            try {
                blockActor.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, settings);

        // Send 'limit + 1' messages: 1 processing, 'limit' in queue.
        for (int i = 0; i <= limit; i++) {
            actor.tell("msg" + i);
        }
        
        // One is being processed, 'limit' are in queue.
        assertThat(actor.getQueuedMessagesAndReplies()).isEqualTo(limit);

        AtomicBoolean tellFinished = new AtomicBoolean(false);
        Thread producer = new Thread(() -> {
            actor.tell("blocking-msg");
            tellFinished.set(true);
        });
        producer.start();

        // Should be blocked
        Thread.sleep(200);
        assertThat(tellFinished.get()).isFalse();
        assertThat(actor.getQueuedMessagesAndReplies()).isEqualTo(limit);

        // Unblock actor
        blockActor.countDown();
        
        // Producer should now finish
        producer.join(1000);
        assertThat(tellFinished.get()).isTrue();
        
        system.shutDown();
    }
}
