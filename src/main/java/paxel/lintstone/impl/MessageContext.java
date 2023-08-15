package paxel.lintstone.impl;

import paxel.lintstone.api.*;

import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Message context instance dedicated for a process of a message or reply
 */
public class MessageContext implements LintStoneMessageEventContext {

    private final ActorSystem actorSystem;
    private final SelfUpdatingActorAccess self;
    private final Object message;
    private final BiConsumer<Object, SelfUpdatingActorAccess> replyHandler;

    public MessageContext(Object message, ActorSystem actorSystem, SelfUpdatingActorAccess self, BiConsumer<Object, SelfUpdatingActorAccess> replyHandler) {
        this.message = message;
        this.actorSystem = actorSystem;
        this.self = self;
        this.replyHandler = replyHandler;
    }

    @Override
    public <T> TypeSafeMonad inCase(Class<T> clazz, LintStoneEventHandler<T> consumer) {
        return new TypeSafeMonad(message, this).inCase(clazz, consumer);
    }

    @Override
    public void otherwise(LintStoneEventHandler<Object> catchAll) {
        new TypeSafeMonad(message, this).otherwise(catchAll);
    }

    @Override
    public void reply(Object msg) throws NoSenderException, UnregisteredRecipientException {
        replyHandler.accept(msg, self);
    }

    @Override
    public void send(String name, Object msg) throws UnregisteredRecipientException {
        Optional<Actor> actor = actorSystem.getActor(name);
        if (!actor.isPresent()) {
            throw new UnregisteredRecipientException("Actor with name " + name + " does not exist");
        }
        actor.get().send(msg, Optional.of(self), null);
    }

    public void ask(String name, Object msg, ReplyHandler handler) throws UnregisteredRecipientException {
        Optional<Actor> actor = actorSystem.getActor(name);
        if (!actor.isPresent()) {
            throw new UnregisteredRecipientException("Actor with name " + name + " does not exist");
        }
        actor.get().send(msg, Optional.of(self), Optional.of(handler));
    }


    @Override
    public LintStoneActorAccess getActor(String name) {
        // give a empty ref, that is filled on demand.
        return new SelfUpdatingActorAccess(name, null, actorSystem, Optional.of(self));
    }





    @Override
    public LintStoneActorAccess registerActor(String name, LintStoneActorFactory factory, Optional<Object> initMessage, ActorSettings settings) {
        return actorSystem.registerActor(name, factory, initMessage, Optional.of(self),settings);
    }

    @Override
    public boolean unregister() {
        return actorSystem.unregisterActor(self.getName());
    }

    @Override
    public String getName() {
        return self.getName();
    }

    @Override
    public boolean unregister(String actorName) {
        return actorSystem.unregisterActor(actorName);
    }
}
