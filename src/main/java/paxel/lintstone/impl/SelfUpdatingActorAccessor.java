package paxel.lintstone.impl;

import paxel.lintstone.api.LintStoneActorAccessor;
import paxel.lintstone.api.ReplyHandler;
import paxel.lintstone.api.UnregisteredRecipientException;

import java.util.concurrent.CompletableFuture;

/**
 * The SelfUpdatingActorAccessor class is an implementation of the LintStoneActorAccessor interface.
 * It provides the ability to send messages to an actor, handle responses to ask() requests,
 * and retrieve information about the actor's status and statistics.
 */
public class SelfUpdatingActorAccessor implements LintStoneActorAccessor {

    private final SelfUpdatingActorAccessor sender;
    private final String name;
    private final ActorSystem system;
    // not volatile as we expect to be used singlethreaded
    private Actor actor;

    SelfUpdatingActorAccessor(String name, Actor actor, ActorSystem system, SelfUpdatingActorAccessor sender) {
        this.name = name;
        this.actor = actor;
        this.system = system;
        this.sender = sender;
    }

    /**
     * Sends a message to an actor for processing.
     *
     * @param message The message to send.
     * @throws UnregisteredRecipientException If the recipient actor is not registered.
     */
    @Override
    public void tell(Object message) throws UnregisteredRecipientException {
        tell(message, sender, null, null);
    }

    /**
     * Sends a message to the Actor represented by this Access. But blocks the call until the number
     * of messages queued is less than the given threshold. If someone else is sending messages to the actor,
     * this call might block forever.
     *
     * @param message The message to send.
     * @param blockThreshold The number of queued messages that causes the call to block.
     * @throws UnregisteredRecipientException in case the actor does not exist.
     */
    @Override
    public void tellWithBackPressure(Object message, int blockThreshold) throws UnregisteredRecipientException {
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

    /**
     * Sends a message to an actor for processing.
     *
     * @param message The message to send.
     * @param sender  The SelfUpdatingActorAccessor of the sender.
     * @throws UnregisteredRecipientException If the recipient actor is not registered.
     */
    public void send(Object message, SelfUpdatingActorAccessor sender) throws UnregisteredRecipientException {
        tell(message, sender, null, null);
    }

    /**
     * Sends a message to an actor for processing.
     *
     * @param message        The message to be sent.
     * @param sender         The SelfUpdatingActorAccessor of the sender.
     * @param replyHandler   The handler for the reply. If null, the message is sent to the sender without relation to the previous message.
     * @param blockThreshold The threshold for back pressure. If null, the message is added to the sequential processor without back pressure.
     * @throws UnregisteredRecipientException If the recipient actor is not registered.
     */
    private void tell(Object message, SelfUpdatingActorAccessor sender, ReplyHandler replyHandler, Integer blockThreshold) throws UnregisteredRecipientException {
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

    private void updateActor() throws UnregisteredRecipientException {
        actor = system.getOptionalActor(name)
                .orElseThrow(() -> new UnregisteredRecipientException("An actor with the name " + name + " is not available"));
    }

    /**
     * Checks if the actor represented by this accessor exists and is valid.
     *
     * @return true if the actor exists and is valid, false otherwise.
     */
    @Override
    public boolean exists() {
        if (actor == null) {
            // fetch new one from system or null if none
            actor = system.getOptionalActor(name).orElse(null);
        }
        return actor != null && actor.isValid();
    }

    /**
     * Sends a message to the actor for processing and provides a handler for the reply.
     *
     * @param message      The message to send.
     * @param replyHandler The handler for the reply.
     * @throws UnregisteredRecipientException If the recipient actor is not registered.
     */
    @Override
    public void ask(Object message, ReplyHandler replyHandler) throws UnregisteredRecipientException {
        // replyHandler is required, therefore not Optional.ofNullable
        tell(message, sender, replyHandler, null);
    }

    /**
     * Sends a message to an actor for processing and provides a {@link CompletableFuture} for the reply.
     *
     * @param message the Message for the actor
     * @param <F> the type of the expected reply
     * @return a {@link CompletableFuture} that represents the result of the ask operation. It will be completed in the context of the asked actor.
     * @throws UnregisteredRecipientException if the recipient actor is not registered
     */
    @Override
    public <F> CompletableFuture<F> ask(Object message) throws UnregisteredRecipientException {
        CompletableFuture<F> result = new CompletableFuture<>();
        tell(message, sender, mec -> mec.otherwise((reply, resultMec) -> {
            try {
                result.complete((F) reply);
            } catch (Exception e) {
                result.completeExceptionally(e);
            }
        }), null);
        return result;
    }


    /**
     * Returns the number of messages currently queued in the actor.
     *
     * @return The number of queued messages.
     */
    @Override
    public int getQueuedMessagesAndReplies() {
        return actor.getQueued();
    }

    /**
     * Returns the number of messages processed by the actor.
     *
     * @return The number of processed messages.
     */
    @Override
    public long getProcessedMessages() {
        return actor.getTotalMessages();
    }

    /**
     * Returns the number of processed replies to ask() requests.
     *
     * @return The number of processed replies.
     */
    @Override
    public long getProcessedReplies() {
        return actor.getTotalReplies();
    }

    /**
     * Returns the name of the actor.
     *
     * @return The name of the actor as a String.
     */
    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return String.valueOf(actor);
    }
}
