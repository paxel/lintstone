package paxel.lintstone.impl;

import paxel.bulkexecutor.SequentialProcessor;
import paxel.lintstone.api.LintStoneActor;
import paxel.lintstone.api.NoSenderException;
import paxel.lintstone.api.ReplyHandler;
import paxel.lintstone.api.UnregisteredRecipientException;

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

    Actor(String name, LintStoneActor actorInstance, SequentialProcessor sequentialProcessor, ActorSystem system, Optional<SelfUpdatingActorAccess> sender) {
        this.name = name;
        this.actorInstance = actorInstance;
        this.sequentialProcessor = sequentialProcessor;
        messageContextFactory = new MessageContextFactory(system, new SelfUpdatingActorAccess(name, this, system, sender));
    }


    boolean isValid() {
        return registered;
    }

    void send(Object message, Optional<SelfUpdatingActorAccess> sender, Optional<ReplyHandler> replyHandler, Integer blockThreshold) throws UnregisteredRecipientException {
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
                if (sender.isPresent()) {
                    sender.get().send(new FailedMessage(message, e, name));
                } else {
                    LOG.log(Level.SEVERE, "While processing " + message + " on " + name + ":", e);
                }
            }
            // TODO: catch exception. introduce error handler.
        };
        if (blockThreshold == null) {
            if (!sequentialProcessor.add(runnable)) {
                throw new IllegalStateException("The sequential processor rejected the message.");
            }
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
    private void handleReply(Object reply, SelfUpdatingActorAccess self, Optional<SelfUpdatingActorAccess> sender, Optional<ReplyHandler> replyHandler) {
        if (replyHandler.isEmpty()) {
            // we don't have to handle this other than just sending it to the sender of the original message.
            sender.orElseThrow(() -> new NoSenderException("Message has no Sender"))
                    .send(reply, self);
        } else if (sender.isPresent()) {
            // we have a reply handler and a sender. so we want the sender to execute the result itself
            sender.get().run(replyHandler.get(), reply);
        } else {
            // result handler without sender. this was asked from outside.
            // we could just execute the runnable here, but then the processing of the msg would be "interrupted" with the processing
            // of the reply. so we enqueue it in ourselves.
            self.run(replyHandler.get(), reply);
        }
    }

    void unregister() {
        registered = false;
    }

    public void run(ReplyHandler replyHandler, Object reply) {
        if (!registered) {
            throw new UnregisteredRecipientException("Actor " + name + " is not registered");
        }
        boolean success = sequentialProcessor.add(() -> {
            // we update the message context with the reply and give it to the reply handler
            MessageContext mec = messageContextFactory.create(reply, (msg, self) -> this.handleReply(msg, self, Optional.empty(), Optional.empty()));
            try {
                replyHandler.process(mec);
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "While processing runnable on " + name + ":", e);
            }
            // TODO: catch exception. introduce error handler.
        });
        if (!success) {
            throw new IllegalStateException("The sequential processor rejected the Runnable.");
        }
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

    public long getTotalMessages() {
        return totalMessages.get();
    }

    public long getTotalReplies() {
        return totalReplies.get();
    }

    public int getQueued() {
        return sequentialProcessor.size();
    }
}
