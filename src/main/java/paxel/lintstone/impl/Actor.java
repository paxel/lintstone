package paxel.lintstone.impl;

import paxel.bulkexecutor.SequentialProcessor;
import paxel.lintstone.api.LintStoneActor;
import paxel.lintstone.api.NoSenderException;
import paxel.lintstone.api.UnregisteredRecipientException;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

class Actor {

    private static final Logger LOG = Logger.getLogger(Actor.class.getName());
    private final String name;

    private final LintStoneActor actorInstance;
    private final SequentialProcessor sequentialProcessor;
    private boolean registered = true;
    private MessageContext mec;

    Actor(String name, LintStoneActor actorInstance, SequentialProcessor sequentialProcessor) {
        this.name = name;
        this.actorInstance = actorInstance;
        this.sequentialProcessor = sequentialProcessor;
    }

    void setMec(MessageContext mec) {
        this.mec = mec;
    }

    boolean isValid() {
        return registered == true;
    }

    void send(Object message, Optional<SelfUpdatingActorAccess> sender, Consumer<Object> responseHandler) throws UnregisteredRecipientException {
        if (!registered) {
            throw new UnregisteredRecipientException("Actor " + name + " is not registered");
        }
        boolean success = sequentialProcessor.add(() -> {
            // update mec and delegate replies to our handleReply method
            mec.init(message, (msg, self) -> {
                this.handleReply(msg, self, sender, responseHandler);
            });
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
            // TODO: catch exception. introduce errorhandler.
        });
        if (!success) {
            throw new IllegalStateException("The sequential processor rejected the message.");
        }
    }

    /**
     * This method decides how to handle responses.
     *
     * @param msg             The msg that was replied
     * @param sender          The sender of the message we reply to
     * @param responseHandler The handler for the response.
     *                        If this is null, we just send the msg to the sender.
     *                        The sender will receive this message in no relation to the previous msg he sent.
     *                        If the responseHandler is given, the relation between msg and response is well defined.
     *                        All reply during the handling of an ask are delegated to the responseHandler.
     */
    private void handleReply(Object msg, SelfUpdatingActorAccess self, Optional<SelfUpdatingActorAccess> sender, Consumer<Object> responseHandler) {
        if (responseHandler == null) {
            // we don't have to handle this other than just sending it to the sender of the original message.
            sender.orElseThrow(() -> new NoSenderException("Message has no Sender"))
                    .send(msg, self);
        } else if (sender.isPresent()) {
            // we have a response handler and a sender. so we want the sender to execute the result itself
            sender.get().run(() -> responseHandler.accept(msg));
        } else {
            // result handler without sender. this was asked from outside. so the actor itself processes the response.
            responseHandler.accept(msg);
        }
    }

    void unregister() {
        registered = false;
    }

    public void run(Runnable message) {
        if (!registered) {
            throw new UnregisteredRecipientException("Actor " + name + " is not registered");
        }
        boolean success = sequentialProcessor.add(() -> {
            // process runnable. It's in the same context as the actor. but without the actual MessageContext
            mec.init(null, (a, b) -> {
                throw new IllegalArgumentException("Can't reply to a ask reply");
            });
            try {
                message.run();
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "While processing runnable on " + name + ":", e);
            }
            // TODO: catch exception. introduce errorhandler.
        });
        if (!success) {
            throw new IllegalStateException("The sequential processor rejected the Runnable.");
        }

    }
}
