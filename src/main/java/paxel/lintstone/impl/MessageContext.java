package paxel.lintstone.impl;

import java.util.Optional;
import paxel.lintstone.api.LintStoneActorAccess;
import paxel.lintstone.api.LintStoneActorFactory;
import paxel.lintstone.api.LintStoneEventHandler;
import paxel.lintstone.api.LintStoneMessageEventContext;
import paxel.lintstone.api.NoSenderException;
import paxel.lintstone.api.UnregisteredRecipientException;

/**
 *
 */
public class MessageContext implements LintStoneMessageEventContext {

    private final Object message;
    private final ActorSystem actorSystem;
    private boolean match;

    public MessageContext(Object message, ActorSystem actorSystem) {
        this.message = message;
        this.actorSystem = actorSystem;
    }

    @Override
    public <T> LintStoneMessageEventContext inCase(Class<T> clazz, LintStoneEventHandler<T> consumer) {
        if (match || clazz.isAssignableFrom(message.getClass())) {
            match = true;
            consumer.handle(clazz.cast(message), this);
        }
        return this;
    }

    @Override
    public void otherwise(LintStoneEventHandler<Object> catchAll) {
        if (!match) {
            catchAll.handle(message, this);
        }
    }

    @Override
    public void reply(Object msg) throws NoSenderException, UnregisteredRecipientException {

    }

    @Override
    public void send(String name, Object msg) throws UnregisteredRecipientException {
        Optional<Actor> actor = actorSystem.getActor(name);
        if (actor.isEmpty()) {
            throw new UnregisteredRecipientException("Actor with name " + name + " does not exist");
        }
        actor.get().send(msg);
    }

    @Override
    public LintStoneActorAccess getActor(String name) {
        // give a empty ref, that is filled on demand.
        return new SelfUpdatingActorAccess(name, null, actorSystem);
    }

    @Override
    public LintStoneActorAccess registerActor(String name, LintStoneActorFactory factory, Optional<Object> initMessage) {
        return actorSystem.registerActor(name, factory, initMessage);
    }

}
