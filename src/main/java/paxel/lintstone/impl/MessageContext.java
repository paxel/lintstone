package paxel.lintstone.impl;

import paxel.lintstone.api.*;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * Message context instance dedicated for a process of a message or reply
 */
public class MessageContext implements LintStoneMessageEventContext {

    private final ActorSystem actorSystem;
    private final SelfUpdatingActorAccessor self;
    private final MessageAccess messageAccess = new MessageAccess();
    private Object message;
    private BiConsumer<Object, SelfUpdatingActorAccessor> replyHandler;

    public MessageContext(ActorSystem actorSystem, SelfUpdatingActorAccessor self) {
        this.actorSystem = actorSystem;
        this.self = self;
    }

    public void reset(Object message, BiConsumer<Object, SelfUpdatingActorAccessor> replyHandler) {
        this.message = message;
        this.replyHandler = replyHandler;
        this.messageAccess.reset(message, this);
    }

    @Override
    public <T> MessageAccess inCase(Class<T> clazz, LintStoneEventHandler<T> consumer) {
        return messageAccess.inCase(clazz, consumer);
    }

    @Override
    public void otherwise(LintStoneEventHandler<Object> catchAll) {
        messageAccess.otherwise(catchAll);
    }

    @Override
    public void reply(Object msg) throws NoSenderException, UnregisteredRecipientException {
        replyHandler.accept(msg, self);
    }

    @Override
    public void tell(String name, Object msg) throws UnregisteredRecipientException {
        Optional<Actor> actor = actorSystem.getOptionalActor(name);
        if (actor.isEmpty()) {
            throw new UnregisteredRecipientException("Actor with name " + name + " does not exist");
        }
        actor.get().send(msg, self, null);
    }

    @Override
    public void tell(String name, Object msg, Duration delay) throws UnregisteredRecipientException {
        Optional<Actor> actor = actorSystem.getOptionalActor(name);
        if (actor.isEmpty()) {
            throw new UnregisteredRecipientException("Actor with name " + name + " does not exist");
        }
        actor.get().send(msg, self, null, delay);
    }

    @Override
    public void ask(String name, Object msg, ReplyHandler handler) throws UnregisteredRecipientException {
        Optional<Actor> actor = actorSystem.getOptionalActor(name);
        if (actor.isEmpty()) {
            throw new UnregisteredRecipientException("Actor with name " + name + " does not exist");
        }
        actor.get().send(msg, self, handler);
    }

    @Override
    public <F> CompletableFuture<F> ask(String name, Object msg) throws UnregisteredRecipientException {
        Optional<Actor> actor = actorSystem.getOptionalActor(name);
        if (actor.isEmpty()) {
            throw new UnregisteredRecipientException("Actor with name " + name + " does not exist");
        }
        CompletableFuture<F> result = new CompletableFuture<>();
        actor.get().send(msg, self, mec -> mec.otherwise((o, m) -> {
            try {
                result.complete((F) o);
            } catch (Exception e) {
                result.completeExceptionally(e);
            }
        }));
        return result;
    }


    @Override
    public LintStoneActorAccessor getActor(String name) {
        // give an empty ref, that is filled on demand.
        return new SelfUpdatingActorAccessor(name, null, actorSystem, self);
    }


    @Override
    public LintStoneActorAccessor registerActor(String name, LintStoneActorFactory factory, Object initMessage, ActorSettings settings) {
        return actorSystem.registerActor(name, factory, self, settings, initMessage);
    }

    @Override
    public LintStoneActorAccessor registerActor(String name, LintStoneActorFactory factory, ActorSettings settings) {
        return actorSystem.registerActor(name, factory, self, settings, null);
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
