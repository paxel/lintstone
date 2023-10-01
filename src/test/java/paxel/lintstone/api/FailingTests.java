package paxel.lintstone.api;

import org.junit.Assert;
import org.junit.Test;
import paxel.lintstone.impl.FailedMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 *
 */
public class FailingTests {

    public static final String ECHO_ACTOR = "echo-actor";
    public static final String GATEWAY = "gateway";
    public static final String STOP_ACTOR = "end-processor";
    public static final String FAILING = "failing";
    public static final String NOT_EXISTANT = "no";
    final CountDownLatch latch = new CountDownLatch(1);

    public FailingTests() {
    }

    @Test
    public void testFailedMessageResponse() throws InterruptedException, ExecutionException {
        LintStoneSystem system = LintStoneSystemFactory.create();
        List<Object> errorMessage = new ArrayList<>();
        system.registerActor(STOP_ACTOR, () -> a -> {
            System.out.println("stop received. countdown latch");
            latch.countDown();
        }, ActorSettings.create().setErrorHandler(errorMessage::add).build());


        // This creates an actor that will create a FAILING actor
        LintStoneActorAccessor stupid = system.registerActor(GATEWAY, StupidActor::new, ActorSettings.create().setErrorHandler(errorMessage::add).build());

        // this will create the error handler
        // send a "Hi" to the error handler
        // that fails
        // the failure causes a FailedMessage in the GATEWAY
        // that will cause the stop
        stupid.send("Don't be stupid");


        boolean await = latch.await(10, TimeUnit.SECONDS);
        if (!await)
            Assert.fail("Timed out without activating latch");

        System.out.println("Test is finished");
        // wait for the result
        System.out.println(system);
        system.shutDownAndWait();
        System.out.println("wait");
        system.shutDownNow();
        System.out.println("now");
        system.shutDown();
        System.out.println("down");
    }

    @Test
    public void testGetDataOut() throws InterruptedException, ExecutionException {
        LintStoneSystem system = LintStoneSystemFactory.create();
        List<Object> errorMessage = new ArrayList<>();


        LintStoneActorAccessor echoActor = system.registerActor(ECHO_ACTOR, () -> a -> a.reply("echo"), ActorSettings.create().setErrorHandler(errorMessage::add).build());

        // this message goes to an actor that wants to reply. but can't, because we are calling from outside the actor system
        // so this should be a message in the errorHandler
        echoActor.send("you ok?");
        // this is the correct way to ask for data from outside the actorSystem
        String echo = echoActor.<String>ask("please tell me").get();
        assertThat(echo, is("echo"));

        // the first try should have created a message here
        // but currently it's just a log message
        assertThat(errorMessage.size(), is(0));

        system.shutDownAndWait();
    }

    private static class StupidActor implements LintStoneActor {

        @Override
        public void newMessageEvent(LintStoneMessageEventContext mec) {
            mec
                    .inCase(String.class, this::handleString)
                    .inCase(FailedMessage.class, this::handleFail)
                    .otherwise((o, m) -> System.out.println("otherwise: " + o));
        }

        private void handleString(String go, LintStoneMessageEventContext mec) {
            LintStoneActorAccessor registered = mec.registerActor(FAILING, () -> m -> {
                // this temporay actor will fail with each message that it receives
                throw new IllegalArgumentException("Go away");
            }, ActorSettings.DEFAULT);

            if (registered.exists()) {
                // the actor is registered, registering it again will not create a new actor but the previous one
                // in that case the "Go away" actor
                LintStoneActorAccessor reRegister = mec.registerActor(FAILING, () -> m -> {
                    // no fail anymore, but this factory will not be called
                }, ActorSettings.DEFAULT);

                if (reRegister.exists()) {
                    // so this first message to the actor should fail and be given to the errorhandler
                    // it also should cause a FailedMessage to be returned to us, that the Message could not be processed
                    reRegister.send("Hi!");
                }
            }
            // We send a message to ourselves, that we don't support
            // the false object will end in the otherwise branch of newMessageEvent
            mec.send(mec.getName(), Boolean.FALSE);
            try {
                mec.send("Unknown Actor", "Will not be delivered");
                throw new IllegalStateException("Should have failed");
            } catch (UnregisteredRecipientException unregisteredRecipientException) {
                // we can't send to unknown actors
            }

            LintStoneActorAccessor actor = mec.getActor(NOT_EXISTANT);

            if (!actor.exists()) {
                try {
                    actor.send("fail me");
                    throw new IllegalStateException("Should have failed");
                } catch (UnregisteredRecipientException unregisteredRecipientException) {
                }
                // register an actor with that name
                mec.registerActor(NOT_EXISTANT, () -> a -> {
                }, ActorSettings.DEFAULT);

                boolean exists = actor.exists();
                // would throw exception if LintStoneActorAccessor is not self updating
                actor.send("This actor reference works now: " + exists);
            }
        }

        private void handleFail(FailedMessage go, LintStoneMessageEventContext m) {
            // The failed message was sent by the temporary actor, because it could not process it
            System.out.println("Failed on " + go.actorName() + " because " + go.cause() + " when processing " + go.message());

            final LintStoneActorAccessor me = m.getActor(m.getName());
            me.send(true);
            // we unregister ourselves
            m.unregister();
            if (me.exists()) {
                throw new IllegalStateException("I was just unregistered");
            }
            try {
                me.send("will not happen");
                throw new IllegalStateException("Should have failed");
            } catch (UnregisteredRecipientException unregisteredRecipientException) {
            }

            // end the test
            m.getActor(STOP_ACTOR).send("stop");
        }
    }

}
