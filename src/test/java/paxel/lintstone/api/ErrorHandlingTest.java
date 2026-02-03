package paxel.lintstone.api;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

public class ErrorHandlingTest {

    @Test
    void testErrorHandlerAbort() throws InterruptedException {
        LintStoneSystem system = LintStoneSystemFactory.create();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Object> lastMsg = new AtomicReference<>();

        ActorSettings settings = ActorSettings.create()
                .setErrorHandler((err, desc, cause) -> ErrorHandlerDecision.ABORT)
                .build();

        LintStoneActorAccessor actor = system.registerActor("actor", () -> mec -> {
            mec.otherwise((msg, ctx) -> {
                if ("fail".equals(msg)) {
                    throw new RuntimeException("intentional failure");
                }
                lastMsg.set(msg);
                latch.countDown();
            });
        }, settings);

        actor.tell("fail");
        actor.tell("msg"); // This should not be processed because actor aborted

        boolean received = latch.await(500, TimeUnit.MILLISECONDS);
        assertThat(received).as("Message after failure should NOT have been received").isFalse();

        system.shutDownNow();
    }

    @Test
    void testErrorHandlerContinue() throws InterruptedException {
        LintStoneSystem system = LintStoneSystemFactory.create();
        CountDownLatch latch = new CountDownLatch(1);

        ActorSettings settings = ActorSettings.create()
                .setErrorHandler((err, desc, cause) -> ErrorHandlerDecision.CONTINUE)
                .build();

        LintStoneActorAccessor actor = system.registerActor("actor", () -> mec -> {
            mec.otherwise((msg, ctx) -> {
                if ("fail".equals(msg)) {
                    throw new RuntimeException("intentional failure");
                }
                latch.countDown();
            });
        }, settings);

        actor.tell("fail");
        actor.tell("msg"); // This SHOULD be processed

        boolean received = latch.await(1, TimeUnit.SECONDS);
        assertThat(received).as("Message after failure SHOULD have been received").isTrue();

        system.shutDownNow();
    }

    @Test
    void testFailedMessagePropagation() throws InterruptedException {
        LintStoneSystem system = LintStoneSystemFactory.create();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<LintStoneFailedMessage> failedMsgRef = new AtomicReference<>();

        system.registerActor("failingActor", () -> mec -> {
            mec.otherwise((msg, ctx) -> {
                throw new RuntimeException("intentional failure");
            });
        }, ActorSettings.DEFAULT);

        system.registerActor("senderActor", () -> mec -> {
            mec.inCase(LintStoneFailedMessage.class, (failed, ctx) -> {
                failedMsgRef.set(failed);
                latch.countDown();
            }).otherwise((msg, ctx) -> {
                ctx.tell("failingActor", "trigger");
            });
        }, ActorSettings.DEFAULT).tell("start");

        boolean received = latch.await(1, TimeUnit.SECONDS);
        assertThat(received).isTrue();
        assertThat(failedMsgRef.get().actorName()).isEqualTo("failingActor");
        assertThat(failedMsgRef.get().message()).isEqualTo("trigger");
        assertThat(failedMsgRef.get().cause()).isInstanceOf(RuntimeException.class);

        system.shutDownNow();
    }
}
