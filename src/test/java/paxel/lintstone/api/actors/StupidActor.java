package paxel.lintstone.api.actors;

import paxel.lintstone.api.*;
import paxel.lintstone.impl.FailedMessage;

public class StupidActor implements LintStoneActor {

    private static void otherwise(Object o, LintStoneMessageEventContext m) {
        System.out.println("otherwise: " + o);
    }

    @Override
    public void newMessageEvent(LintStoneMessageEventContext mec) {
        mec
                .inCase(String.class, this::handleString)
                .inCase(FailedMessage.class, this::handleFail)
                .otherwise(StupidActor::otherwise);
    }

    private void handleString(String go, LintStoneMessageEventContext mec) {
        LintStoneActorAccessor registered = mec.registerActor(FailingTests.FAILING, () -> m -> {
            m.otherwise((msg, ctx) -> {
                // this temporary actor will fail with each message that it receives
                throw new IllegalArgumentException("Go away");
            });
        }, ActorSettings.DEFAULT);

        if (registered.exists()) {
            // the actor is registered, registering it again will not create a new actor but the previous one
            // in that case the "Go away" actor
            LintStoneActorAccessor reRegister = mec.registerActor(FailingTests.FAILING, () -> m -> {
                // no fail anymore, but this factory will not be called
            }, ActorSettings.DEFAULT);

            if (reRegister.exists()) {
                // so this first message to the actor should fail and be given to the errorhandler
                // it also should cause a FailedMessage to be returned to us, that the Message could not be processed
                reRegister.tell("Hi!");
            }
        }
        // We send a message to ourselves, that we don't support
        // the false object will end in the otherwise branch of newMessageEvent
        mec.tell(mec.getName(), Boolean.FALSE);
        try {
            mec.tell("Unknown Actor", "Will not be delivered");
            throw new IllegalStateException("Should have failed");
        } catch (UnregisteredRecipientException unregisteredRecipientException) {
            // we can't send to unknown actors
        }

        LintStoneActorAccessor actor = mec.getActor(FailingTests.NOT_EXISTENT);

        if (!actor.exists()) {
            try {
                actor.tell("fail me");
                throw new IllegalStateException("Should have failed");
            } catch (UnregisteredRecipientException unregisteredRecipientException) {
            }
            // register an actor with that name
            mec.registerActor(FailingTests.NOT_EXISTENT, () -> a -> a.otherwise((msg, ctx) -> {
            }), ActorSettings.DEFAULT);

            boolean exists = actor.exists();
            // would throw exception if LintStoneActorAccessor is not self updating
            actor.tell("This actor reference works now: " + exists);
        }
    }

    private void handleFail(FailedMessage go, LintStoneMessageEventContext m) {
        // The failed message was sent by the temporary actor, because it could not process it
        System.out.println("Failed on " + go.actorName() + " because " + go.cause() + " when processing " + go.message());

        final LintStoneActorAccessor me = m.getActor(m.getName());
        me.tell(true);
        // we unregister ourselves
        m.unregister();
        if (me.exists()) {
            throw new IllegalStateException("I was just unregistered");
        }
        try {
            me.tell("will not happen");
            throw new IllegalStateException("Should have failed");
        } catch (UnregisteredRecipientException unregisteredRecipientException) {
        }

        // end the test
        m.getActor(FailingTests.STOP_ACTOR).tell("stop");
    }
}
