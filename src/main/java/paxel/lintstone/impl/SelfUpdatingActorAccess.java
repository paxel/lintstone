package paxel.lintstone.impl;

import java.util.Optional;
import java.util.function.Consumer;

import paxel.lintstone.api.LintStoneActorAccess;
import paxel.lintstone.api.UnregisteredRecipientException;

/**
 * This ActorAccess will try to fetch a new instance of an actor in case the
 * current one becomes invalid.
 */
public class SelfUpdatingActorAccess implements LintStoneActorAccess {

    private final Optional<SelfUpdatingActorAccess> sender;
    private final String name;
    private final ActorSystem system;
    // not volatile as we expect to be used singlethreaded
    private Actor actor;

    SelfUpdatingActorAccess(String name, Actor actor, ActorSystem system, Optional<SelfUpdatingActorAccess> sender) {
        this.name = name;
        this.actor = actor;
        this.system = system;
        this.sender = sender;
    }

    @Override
    public void send(Object message) throws UnregisteredRecipientException {
        tell(message, sender, null);
    }

    /**
     * This is an internal method to delegate Runnables to an actor. Mainly this is used to handle Responses to ask() in the correct thread.
     *
     * @param runnable The runnable that has to be processed by the actor.
     * @throws UnregisteredRecipientException In case the actor has been unregistered.
     */
    void run(Runnable runnable) throws UnregisteredRecipientException {
        if (actor == null) {
            updateActor();
        }
        try {
            actor.run(runnable);
        } catch (UnregisteredRecipientException ignoredOnce) {
            actor = null;
            updateActor();
            // second try throws the exception to the outside, in case the actor provided was already unregistered.
            actor.run(runnable);
        }
    }

    public void send(Object message, SelfUpdatingActorAccess sender) throws UnregisteredRecipientException {
        tell(message, Optional.ofNullable(sender), null);
    }

    private void tell(Object message, Optional<SelfUpdatingActorAccess> sender, Consumer<Object> responseHandler) throws UnregisteredRecipientException {
        if (actor == null) {
            updateActor();
        }
        try {
            actor.send(message, sender, responseHandler);
        } catch (UnregisteredRecipientException ignoredOnce) {
            actor = null;
            updateActor();
            // second try throws ,the exception to the outside, in case the actore provided was already unregistered.
            actor.send(message, sender, responseHandler);
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

    @Override
    public void ask(Object message, Consumer<Object> responseHandler) throws UnregisteredRecipientException {
        tell(message, sender, responseHandler);
    }

    String getName() {
        return name;
    }

}
