package paxel.lintstone.impl;

import java.util.Optional;
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
        sequentialProcessor.add(() -> {
            // update mec
            mec.init(message, sender);
            // process message
            try {
                actorInstance.newMessageEvent(mec);
            } catch (Exception e) {
                sender.ifPresent(s -> {
                    s.send(new FailedMessage(message, e, name));
                });
            }
            // TODO: catch exception. introduce errorhandler.
        });
    }

    void unregister() {
        registered = false;
    }

}
