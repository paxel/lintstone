package paxel.lintstone.impl;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import paxel.bulkexecutor.SequentialProcessor;
import paxel.lintstone.api.LintStoneActor;
import paxel.lintstone.api.UnregisteredRecipientException;

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

    void send(Object message, Optional<SelfUpdatingActorAccess> sender) throws UnregisteredRecipientException {
        if (!registered) {
            throw new UnregisteredRecipientException("Actor " + name + " is not registered");
        }
        boolean success = sequentialProcessor.add(() -> {
            // update mec
            mec.init(message, sender);
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

    void unregister() {
        registered = false;
    }

}
