package paxel.lintstone.api;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.Test;
import paxel.lintstone.impl.FailedMessage;

/**
 *
 */
public class FailingTests {
    
    private static final Random R = new Random(0xbadbee);
    CountDownLatch latch = new CountDownLatch(1);
    
    public FailingTests() {
    }
    
    @Test
    public void testSomeMethod() throws InterruptedException {
        LintStoneSystem system = LintStoneSystemFactory.create(Executors.newWorkStealingPool());
        LintStoneActorAccess peepee = system.registerActor("peepee", () -> new StupidActor(), Optional.of("Go"));
        LintStoneActorAccess stopper = system.registerActor("floor", () -> a -> latch.countDown(), Optional.empty());
        
        LintStoneActorAccess poop = system.registerActor("poop", () -> a -> {
            a.reply("nope");
        }, Optional.empty());
        
        poop.send("you ok?");
        
        latch.await();
        // wait for the result
        system.shutDownAndWait();
        system.shutDownNow();
        system.shutDown();
    }
    
    private static class StupidActor implements LintStoneActor {
        
        @Override
        public void newMessageEvent(LintStoneMessageEventContext mec) {
            mec
                    .inCase(String.class, (go, m) -> this.fuckUp(go, m))
                    .inCase(FailedMessage.class, (go, m) -> this.failed(go, m))
                    .otherwise((o, m) -> {
                        System.out.println("otherwise: " + o);
                    });
        }
        
        private void fuckUp(String go, LintStoneMessageEventContext mec) {
            LintStoneActorAccess registered = mec.registerActor("dala", () -> m -> {
                throw new IllegalArgumentException("Go away");
            }, Optional.empty());
            
            if (registered.exists()) {
                LintStoneActorAccess second = mec.registerActor("dala", () -> m -> {
                    System.out.print("I am ignored");
                }, Optional.empty());
                
                if (second.exists()) {
                    // will fail on the other actor and produce a failed message for us.
                    second.send("Hi!");
                }
            }
            // will cause otherwise
            mec.send(mec.getName(), Boolean.FALSE);
            try {
                mec.send("Unknown Actor", "Will not be delivered");
                throw new IllegalStateException("Should have failed");
            } catch (UnregisteredRecipientException unregisteredRecipientException) {
            }
            
            LintStoneActorAccess actor = mec.getActor("no");
            
            if (!actor.exists()) {
                try {
                    actor.send("fail me");
                    throw new IllegalStateException("Should have failed");
                } catch (UnregisteredRecipientException unregisteredRecipientException) {
                }
            }
        }
        
        private void failed(FailedMessage go, LintStoneMessageEventContext m) {
            System.out.println("Failed on " + go.getActorName() + " because " + go.getCause() + " when processing " + go.getMessage());
            
            final LintStoneActorAccess me = m.getActor(m.getName());
            me.send(true);
            // we unregister ourselves
            m.unregister();
            if (me.exists()) {
                throw new IllegalArgumentException("fwef");
            }
            try {
                me.send("will not happen");
                throw new IllegalStateException("Should have failed");
            } catch (UnregisteredRecipientException unregisteredRecipientException) {
            }
            
            LintStoneActorAccess oldActor = m.registerActor("someOne", () -> a -> {
                
            }, Optional.empty());
            oldActor.send("lala");
            // unregister that one
            if (m.unregister("someOne")) {
                // register a new one
                m.registerActor("someOne", () -> a -> {
                    
                }, Optional.empty());
                
                oldActor.send("works");
                
                m.getActor("floor").send("sztop");
            }
        }
    }
    
}
