package paxel.lintstone.impl;

import paxel.bulkexecutor.SequentialProcessor;
import paxel.lintstone.api.LintStoneActor;
import paxel.lintstone.api.UnregisteredRecipientException;

/**
 *
 */
class Actor {

    private final LintStoneActor actorInstance;
    private final SequentialProcessor sequentialProcessor;
    private boolean registered = true;
    private final ActorSystem actorSystem;

    Actor(LintStoneActor actorInstance, SequentialProcessor sequentialProcessor, ActorSystem actorSystem) {
        this.actorInstance = actorInstance;
        this.sequentialProcessor = sequentialProcessor;
        this.actorSystem = actorSystem;
    }

    boolean isValid() {
        return registered = true;
    }

    void send(Object message) throws UnregisteredRecipientException {

        sequentialProcessor.add(() -> actorInstance.newMessageEvent(new MessageContext(message, actorSystem)));
    }

    void unregister() {
        registered = false;
    }

}
