package paxel.lintstone.api;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;

public class DelayedMessageTest {

    @Test
    void testDelayedMessage() throws InterruptedException {
        LintStoneSystem system = LintStoneSystemFactory.create();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicLong receiveTime = new AtomicLong(0);
        long startTime = System.currentTimeMillis();

        system.registerActor("target", () -> mec -> {
            mec.otherwise((msg, ctx) -> {
                receiveTime.set(System.currentTimeMillis());
                latch.countDown();
            });
        }, ActorSettings.DEFAULT);

        system.registerActor("scheduler", () -> mec -> {
            mec.otherwise((msg, ctx) -> {
                ctx.tell("target", "msg", Duration.ofMillis(500));
            });
        }, ActorSettings.DEFAULT).tell("start");

        boolean received = latch.await(2, TimeUnit.SECONDS);
        assertThat(received, is(true));

        long delay = receiveTime.get() - startTime;
        assertThat("Delay should be at least 500ms, was " + delay, delay >= 500, is(true));

        system.shutDownNow();
    }

    @Test
    void testDelayedMessageNotSentIfUnregistered() throws InterruptedException {
        LintStoneSystem system = LintStoneSystemFactory.create();
        CountDownLatch latch = new CountDownLatch(1);

        system.registerActor("target", () -> mec -> {
            mec.otherwise((msg, ctx) -> {
                latch.countDown();
            });
        }, ActorSettings.DEFAULT);

        system.registerActor("scheduler", () -> mec -> {
            mec.otherwise((msg, ctx) -> {
                ctx.tell("target", "msg", Duration.ofMillis(500));
            });
        }, ActorSettings.DEFAULT).tell("start");

        // Unregister target before it's sent
        Thread.sleep(100);
        system.unregisterActor("target");

        boolean received = latch.await(1, TimeUnit.SECONDS);
        assertThat("Message should NOT have been received", received, is(false));

        system.shutDownNow();
    }
}
