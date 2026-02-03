package paxel.lintstone.api;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ActorLifecycleTest {

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
        assertThat(unregistered, is(true));
        
        // After unregister, tell should throw exception
        assertThrows(UnregisteredRecipientException.class, () -> actor.tell("msg3"));
        
        // Wait a bit to ensure queued messages are processed
        Thread.sleep(100);
        
        // Queued messages should still be processed
        assertThat(processCount.get(), is(2));
        
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
        Thread.sleep(100);
        
        assertThat(unregisteredRef.get(), is(true));
        assertThat(actor.exists(), is(false));
        
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
        
        Thread.sleep(100);
        
        assertThat(actor1Count.get(), is(1));
        assertThat(actor2Count.get(), is(1));
        
        system.shutDownNow();
    }
}
