package paxel.lintstone.api;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ActorLifecycleTest {

    private void waitForAtomicInteger(AtomicInteger atomic, int expected) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (atomic.get() < expected && System.currentTimeMillis() - start < 5000) {
            Thread.sleep(10);
        }
    }

    @Test
    void testUnregisterActor() throws InterruptedException {
        LintStoneSystem system = LintStoneSystemFactory.create();
        AtomicInteger processCount = new AtomicInteger(0);

        LintStoneActorAccessor actor = system.registerActor("actor", () -> new LintStoneActor() {
            @Override
            public void newMessageEvent(LintStoneMessageEventContext mec) {
                mec.otherwise((msg, ctx) -> {
                    processCount.incrementAndGet();
                });
            }
        }, ActorSettings.DEFAULT);

        actor.tell("msg1");
        actor.tell("msg2");
        
        boolean unregistered = system.unregisterActor("actor");
        assertThat(unregistered).isTrue();
        
        // After unregister, tell should throw exception
        assertThrows(UnregisteredRecipientException.class, () -> actor.tell("msg3"));
        
        // Wait to ensure queued messages are processed
        waitForAtomicInteger(processCount, 2);
        
        // Queued messages should still be processed
        assertThat(processCount.get()).isEqualTo(2);
        
        system.shutDownNow();
    }

    @Test
    void testSelfUnregister() throws InterruptedException {
        LintStoneSystem system = LintStoneSystemFactory.create();
        AtomicBoolean unregisteredRef = new AtomicBoolean(false);

        LintStoneActorAccessor actor = system.registerActor("actor", () -> new LintStoneActor() {
            @Override
            public void newMessageEvent(LintStoneMessageEventContext mec) {
                mec.otherwise((msg, ctx) -> {
                    unregisteredRef.set(ctx.unregister());
                });
            }
        }, ActorSettings.DEFAULT);

        actor.tell("die");
        
        // Wait for processing
        long start = System.currentTimeMillis();
        while (!unregisteredRef.get() && System.currentTimeMillis() - start < 5000) {
            Thread.sleep(10);
        }
        
        assertThat(unregisteredRef.get()).isTrue();
        assertThat(actor.exists()).isFalse();
        
        system.shutDownNow();
    }

    @Test
    void testReRegisterActor() throws InterruptedException {
        LintStoneSystem system = LintStoneSystemFactory.create();
        AtomicInteger actor1Count = new AtomicInteger(0);
        AtomicInteger actor2Count = new AtomicInteger(0);

        system.registerActor("actor", () -> new LintStoneActor() {
            @Override
            public void newMessageEvent(LintStoneMessageEventContext mec) {
                mec.otherwise((msg, ctx) -> actor1Count.incrementAndGet());
            }
        }, ActorSettings.DEFAULT);

        LintStoneActorAccessor accessor = system.getActor("actor");
        accessor.tell("msg");

        system.unregisterActor("actor");

        // Re-register with same name but different implementation
        system.registerActor("actor", () -> new LintStoneActor() {
            @Override
            public void newMessageEvent(LintStoneMessageEventContext mec) {
                mec.otherwise((msg, ctx) -> actor2Count.incrementAndGet());
            }
        }, ActorSettings.DEFAULT);
        
        // The accessor should now point to the new actor
        accessor.tell("msg");
        
        waitForAtomicInteger(actor1Count, 1);
        waitForAtomicInteger(actor2Count, 1);
        
        assertThat(actor1Count.get()).isEqualTo(1);
        assertThat(actor2Count.get()).isEqualTo(1);
        
        system.shutDownNow();
    }
}
