package paxel.lintstone.impl;

import paxel.lintstone.api.LintStoneActorAccess;
import paxel.lintstone.api.UnregisteredRecipientException;

/**
 * This ActorAccess will try to fetch a new instance of an actor in case the
 * current one becomes invalid.
 */
public class SelfUpdatingActorAccess implements LintStoneActorAccess {

    private final String name;
    private final ActorSystem system;
    // not volatile as we expect to be used singlethreaded
    private Actor actor;

    SelfUpdatingActorAccess(String name, Actor actor, ActorSystem system) {
        this.name = name;
        this.actor = actor;
        this.system = system;
    }

    @Override
    public void send(Object message) throws UnregisteredRecipientException {
        if (actor == null) {
            updateActor();
        }
        try {
            actor.send(message);
        } catch (UnregisteredRecipientException ignoredOnce) {
            actor = null;
            updateActor();
            // second try throws the exception to the outside, in case the actore provided was already unregistered.
            actor.send(message);
        }
    }

    private void updateActor() throws UnregisteredRecipientException {
        actor = system.getActor(name)
                .orElseThrow(() -> new UnregisteredRecipientException("An actor with the name " + name + " is not available"));
    }

    @Override
    public boolean exists() {
        if (actor == null) {
            // fetch new one from system or null if none
            actor = system.getActor(name).orElse(null);
        }
        return actor != null && actor.isValid();
    }

}
