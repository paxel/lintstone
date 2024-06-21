package paxel.lintstone.impl;

import paxel.lintstone.api.LintStoneActorAccessor;
import paxel.lintstone.api.ReplyHandler;
import paxel.lintstone.api.UnregisteredRecipientException;

import java.util.concurrent.CompletableFuture;

/**
 * This ActorAccess will try to fetch a new instance of an actor in case the
 * current one becomes invalid.
 */
public class SelfUpdatingActorAccessor implements LintStoneActorAccessor {

    private final SelfUpdatingActorAccessor sender;
    private final String name;
    private final ActorSystem system;
    // not volatile as we expect to be used single-threaded
    private Actor actor;

    SelfUpdatingActorAccessor(String name, Actor actor, ActorSystem system, SelfUpdatingActorAccessor sender) {
        this.name = name;
        this.actor = actor;
        this.system = system;
        this.sender = sender;
    }

    @Override
    public void tell(Object message) throws UnregisteredRecipientException {
        tell(message, sender, null);
    }

    @Override
    public void tellWithBackPressure(Object message, int blockThreshold) throws UnregisteredRecipientException, InterruptedException {
        tell(message, sender, null, blockThreshold);
    }

    /**
     * This is an internal method to delegate Runnables to an actor. Mainly this is used to handle Responses to ask() in the correct thread.
     *
     * @param runnable The runnable that has to be processed by the actor.
     * @throws UnregisteredRecipientException In case the actor has been unregistered.
     */
    void run(ReplyHandler runnable, Object reply) throws UnregisteredRecipientException {
        if (actor == null) {
            updateActor();
        }
        try {
            actor.run(runnable, reply);
        } catch (UnregisteredRecipientException ignoredOnce) {
            actor = null;
            updateActor();
            // second try throws the exception to the outside, in case the actor provided was already unregistered.
            actor.run(runnable, reply);
        }
    }

    public void send(Object message, SelfUpdatingActorAccessor sender) throws UnregisteredRecipientException {
        tell(message, sender, null);
    }

    private void tell(Object message, SelfUpdatingActorAccessor sender, ReplyHandler replyHandler, Integer blockThreshold) throws UnregisteredRecipientException, InterruptedException {
        if (actor == null) {
            updateActor();
        }
        try {
            actor.send(message, sender, replyHandler, blockThreshold);
        } catch (UnregisteredRecipientException ignoredOnce) {
            actor = null;
            updateActor();
            // second try throws the exception to the outside, in case the actor provided was already unregistered.
            actor.send(message, sender, replyHandler, blockThreshold);
        }
    }

    private void tell(Object message, SelfUpdatingActorAccessor sender, ReplyHandler replyHandler) throws UnregisteredRecipientException {
        if (actor == null) {
            updateActor();
        }
        try {
            actor.send(message, sender, replyHandler);
        } catch (UnregisteredRecipientException ignoredOnce) {
            actor = null;
            updateActor();
            // second try throws the exception to the outside, in case the actor provided was already unregistered.
            actor.send(message, sender, replyHandler);
        }
    }


    private void updateActor() throws UnregisteredRecipientException {
        actor = system.getOptionalActor(name)
                .orElseThrow(() -> new UnregisteredRecipientException("An actor with the name " + name + " is not available"));
    }

    @Override
    public boolean exists() {
        if (actor == null) {
            // fetch new one from system or null if none
            actor = system.getOptionalActor(name).orElse(null);
        }
        return actor != null && actor.isValid();
    }

    @Override
    public void ask(Object message, ReplyHandler replyHandler) throws UnregisteredRecipientException {
        // replyHandler is required, therefore not Optional.ofNullable
        tell(message, sender, replyHandler);
    }

    @Override
    public <F> CompletableFuture<F> ask(Object message) throws UnregisteredRecipientException {
        CompletableFuture<F> result = new CompletableFuture<>();
        tell(message, sender, mec -> mec.otherwise((reply, resultMec) -> {
            try {
                result.complete((F) reply);
            } catch (Exception e) {
                result.completeExceptionally(e);
            }
        }));
        return result;
    }


    @Override
    public int getQueuedMessagesAndReplies() {
        return actor.getQueued();
    }

    @Override
    public long getProcessedMessages() {
        return actor.getTotalMessages();
    }

    @Override
    public long getProcessedReplies() {
        return actor.getTotalReplies();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return String.valueOf(actor);
    }
}
