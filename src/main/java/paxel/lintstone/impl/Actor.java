package paxel.lintstone.impl;

import paxel.lintstone.api.*;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This wraps the actual implementation of an Actor and makes sure that inside the actor system everything
 * is well synchronized.
 */
class Actor {

    private static final Logger LOG = Logger.getLogger(Actor.class.getName());
    private final String name;

    private final LintStoneActor actorInstance;
    private final SequentialProcessor sequentialProcessor;
    private volatile boolean registered = true;
    private final AtomicLong totalMessages = new AtomicLong();
    private final AtomicLong totalReplies = new AtomicLong();
    private final MessageContextFactory messageContextFactory;

    /**
     * Constructs a new Actor with the given parameters.
     *
     * @param name                  The name of the actor.
     * @param actorInstance         The instance of the LintStoneActor interface that this actor represents.
     * @param sequentialProcessor   The SequentialProcessor responsible for processing messages sequentially.
     * @param system                The ActorSystem that manages this actor.
     * @param sender                The SelfUpdatingActorAccessor of the sender.
     */
    Actor(String name, LintStoneActor actorInstance, SequentialProcessor sequentialProcessor, ActorSystem system, SelfUpdatingActorAccessor sender) {
        this.name = name;
        this.actorInstance = actorInstance;
        this.sequentialProcessor = sequentialProcessor;
        messageContextFactory = new MessageContextFactory(system, new SelfUpdatingActorAccessor(name, this, system, sender));
    }


    /**
     * Checks if the actor is valid.
     *
     * @return true if the actor is valid, false otherwise.
     */
    boolean isValid() {
        return registered;
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
    void send(Object message, SelfUpdatingActorAccessor sender, ReplyHandler replyHandler, Integer blockThreshold) throws UnregisteredRecipientException {
        if (!registered) {
            throw new UnregisteredRecipientException("Actor " + name + " is not registered");
        }


        Runnable runnable = () -> {
            // create mec and delegate replies to our handleReply method
            MessageContext mec = messageContextFactory.create(message, (msg, self) -> this.handleReply(msg, self, sender, replyHandler));
            // process message
            try {
                actorInstance.newMessageEvent(mec);
            } catch (Exception e) {
                if (sender != null) {
                    sender.tell(new FailedMessage(message, e, name));
                } else {
                    LOG.log(Level.SEVERE, "While processing " + message + " on " + name + ":", e);
                }
            }
            // TODO: catch exception. introduce error handler.
        };
        if (blockThreshold == null) {
            sequentialProcessor.add(runnable);
        } else {
            if (!sequentialProcessor.addWithBackPressure(runnable, blockThreshold)) {
                throw new IllegalStateException("The sequential processor rejected the message.");
            }
        }
        totalMessages.incrementAndGet();
    }

    /**
     * This method decides how to handle replys.
     *
     * @param reply        The msg that was replied
     * @param sender       The sender of the message we reply to
     * @param replyHandler The handler for the reply.
     *                     If this is null, we just send the msg to the sender.
     *                     The sender will receive this message in no relation to the previous msg he sent.
     *                     If the replyHandler is given, the relation between msg and reply is well defined.
     *                     All reply during the handling of an ask are delegated to the replyHandler.
     */
    private void handleReply(Object reply, SelfUpdatingActorAccessor self, SelfUpdatingActorAccessor sender, ReplyHandler replyHandler) {
        if (replyHandler == null) {
            // we don't have to handle this other than just sending it to the sender of the original message.
            Optional.ofNullable(sender)
                    .orElseThrow(() -> new NoSenderException("Message has no Sender"))
                    .send(reply, self);
        } else if (sender != null) {
            // we have a reply handler and a sender. so we want the sender to execute the result itself
            sender.run(replyHandler, reply);
        } else {
            // result handler without sender. this was asked from outside.
            // we could just execute the runnable here, but then the processing of the msg would be "interrupted" with the processing
            // of the reply. so we enqueue it in ourselves.
            self.run(replyHandler, reply);
        }
    }

    /**
     * Unregisters the actor gracefully. This method sets the 'registered' flag to false
     * and calls the unregisterGracefully() method of the sequential processor associated with the actor.
     */
    void unregisterGracefully() {
        registered = false;
        sequentialProcessor.unregisterGracefully();
    }

    /**
     * Shuts down the sequential processor.
     *
     * @param now true if the shutdown should happen immediately, false if it should be graceful
     */
    void shutdown(boolean now) {
        sequentialProcessor.shutdown(now);
    }

    /**
     * Executes the specified reply handler with the given reply object.
     *
     * @param replyHandler The handler for the reply.
     * @param reply        The reply object.
     * @throws UnregisteredRecipientException If the actor is not registered.
     */
    public void run(ReplyHandler replyHandler, Object reply) {
        if (!registered) {
            throw new UnregisteredRecipientException("Actor " + name + " is not registered");
        }
        sequentialProcessor.add(() -> {
            // we update the message context with the reply and give it to the reply handler
            MessageContext mec = messageContextFactory.create(reply, (msg, self) -> this.handleReply(msg, self, null, null));
            try {
                replyHandler.process(mec);
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "While processing runnable on " + name + ":", e);
            }
            // TODO: catch exception. introduce error handler.
        });
        totalReplies.incrementAndGet();
    }

    @Override
    public String toString() {
        return "Actor{" +
                "name='" + name + '\'' +
                " registered='" + registered + '\'' +
                " total='" + totalMessages.get() + '\'' +
                " queued='" + sequentialProcessor.size() + '\'' +
                '}';
    }

    /**
     * Returns the total number of messages processed by the actor.
     *
     * @return The total number of messages processed.
     */
    public long getTotalMessages() {
        return totalMessages.get();
    }

    /**
     * Returns the total number of replies processed by the actor.
     *
     * @return The total number of replies processed.
     */
    public long getTotalReplies() {
        return totalReplies.get();
    }

    /**
     * Returns the number of messages currently queued in the SequentialProcessor.
     *
     * @return The number of queued messages.
     */
    public int getQueued() {
        return sequentialProcessor.size();
    }
}
