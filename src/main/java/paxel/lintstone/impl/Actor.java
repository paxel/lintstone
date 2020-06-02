package paxel.lintstone.impl;

import java.util.Optional;
import java.util.logging.Logger;
import paxel.bulkexecutor.SequentialProcessor;
import paxel.lintstone.api.LintStoneActor;
import paxel.lintstone.api.NoSenderException;
import paxel.lintstone.api.UnregisteredRecipientException;

/**
 *
 */
class Actor {

    private final LintStoneActor actorInstance;
    private final SequentialProcessor sequentialProcessor;
    private boolean registered = true;
    private final MessageContext mec;

    Actor(LintStoneActor actorInstance, SequentialProcessor sequentialProcessor, MessageContext gmec) {
        this.actorInstance = actorInstance;
        this.sequentialProcessor = sequentialProcessor;
        this.mec = gmec;
    }

    boolean isValid() {
        return registered == true;
    }

    void send(Object message, Optional< SelfUpdatingActorAccess> sender) throws UnregisteredRecipientException {
        sequentialProcessor.add(() -> {
            // update mec
            mec.setMessage(message);
            mec.setSender(sender);
            // process message
            actorInstance.newMessageEvent(mec);
            // TODO: catch exception. introduce errorhandler.
        });
    }
    private static final Logger LOG = Logger.getLogger(Actor.class.getName());

    void unregister() {
        registered = false;
    }

}
