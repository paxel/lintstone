package paxel.lintstone.impl;

import paxel.lintstone.api.*;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * Represents the context for a message event. It provides methods for handling messages, replying to senders, sending messages to actors, and registering/unregistering actors.
 */
public class MessageContext implements LintStoneMessageEventContext {

    private final ActorSystem actorSystem;
    private final SelfUpdatingActorAccessor self;
    private final Object message;
    private final BiConsumer<Object, SelfUpdatingActorAccessor> replyHandler;

    /**
     * Represents the context of a message being processed by an actor system.
     */
    public MessageContext(Object message, ActorSystem actorSystem, SelfUpdatingActorAccessor self, BiConsumer<Object, SelfUpdatingActorAccessor> replyHandler) {
        this.message = message;
        this.actorSystem = actorSystem;
        this.self = self;
        this.replyHandler = replyHandler;
    }

    /**
     * Executes the given event handler if the message class is assignable from the specified class.
     * The returned TypeSafeMonad instance will be non-functional if the handler is executed,
     * otherwise, it returns itself.
     *
     * @param clazz    The class to check assignability.
     * @param consumer The event handler for the class.
     * @return The updated TypeSafeMonad instance.
     * @param <T>      The type of the class.
     */
    @Override
    public <T> TypeSafeMonad inCase(Class<T> clazz, LintStoneEventHandler<T> consumer) {
        return new TypeSafeMonad(message, this).inCase(clazz, consumer);
    }

    /**
     * Executes the provided event handler if no match is found for the message class.
     *
     * @param catchAll The handler for unknown types.
     */
    @Override
    public void otherwise(LintStoneEventHandler<Object> catchAll) {
        new TypeSafeMonad(message, this).otherwise(catchAll);
    }

    /**
     * Sends a reply to the sender of the message.
     *
     * @param msg the message to reply.
     * @throws NoSenderException               if there is no sender for the current message.
     * @throws UnregisteredRecipientException if there is no actor with that name.
     */
    @Override
    public void reply(Object msg) throws NoSenderException, UnregisteredRecipientException {
        replyHandler.accept(msg, self);
    }

    /**
     * Sends a message to the actor with the given name.
     *
     * @param name the name of the actor.
     * @param msg  The message to send.
     * @throws UnregisteredRecipientException if there is no actor with that name.
     */
    @Override
    public void tell(String name, Object msg) throws UnregisteredRecipientException {
        Optional<Actor> actor = actorSystem.getOptionalActor(name);
        if (actor.isEmpty()) {
            throw new UnregisteredRecipientException("Actor with name " + name + " does not exist");
        }
        actor.get().send(msg, self, null, null);
    }

    /**
     * Sends a message to the actor with the given name, expecting a reply.
     *
     * @param name    the name of the actor.
     * @param msg     The message to send.
     * @param handler The reply handler.
     * @throws UnregisteredRecipientException if there is no actor with that name.
     */
    @Override
    public void ask(String name, Object msg, ReplyHandler handler) throws UnregisteredRecipientException {
        Optional<Actor> actor = actorSystem.getOptionalActor(name);
        if (actor.isEmpty()) {
            throw new UnregisteredRecipientException("Actor with name " + name + " does not exist");
        }
        actor.get().send(msg, self, handler, null);
    }

    /**
     * Sends a message to the actor with the given name and expects a reply.
     *
     * @param name the name of the actor.
     * @param msg  The message to send.
     * @param <F>  The type of the expected reply.
     * @return a CompletableFuture that represents the expected reply from the actor.
     * @throws UnregisteredRecipientException if there is no actor registered with the given name.
     */
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
        }), null);
        return result;
    }


    /**
     * Retrieves a LintStoneActorAccessor for the specified actor name.
     *
     * @param name The name of the actor.
     * @return A LintStoneActorAccessor for the specified actor name.
     */
    @Override
    public LintStoneActorAccessor getActor(String name) {
        // give a empty ref, that is filled on demand.
        return new SelfUpdatingActorAccessor(name, null, actorSystem, self);
    }


    /**
     * Registers an actor with the given name, factory, init message, and settings.
     *
     * @param name        The name of the actor.
     * @param factory     The factory used to create the actor.
     * @param initMessage The initial message to send to the actor.
     * @param settings    The settings for the actor.
     * @return A LintStoneActorAccessor for the registered actor.
     */
    @Override
    public LintStoneActorAccessor registerActor(String name, LintStoneActorFactory factory, Object initMessage, ActorSettings settings) {
        return actorSystem.registerActor(name, factory, self, settings, initMessage);
    }

    /**
     * Registers an actor with the specified name, factory, and settings.
     *
     * @param name     The name of the actor.
     * @param factory  The factory used to create the actor.
     * @param settings The settings for the actor.
     * @return A LintStoneActorAccessor for the registered actor.
     */
    @Override
    public LintStoneActorAccessor registerActor(String name, LintStoneActorFactory factory, ActorSettings settings) {
        return actorSystem.registerActor(name, factory, self, settings, null);
    }

    /**
     * Unregisters the actor from the system.
     *
     * @return true if the actor is successfully unregistered, false otherwise.
     */
    @Override
    public boolean unregister() {
        return actorSystem.unregisterActor(self.getName());
    }

    /**
     * Retrieves the name associated with this MessageContext.
     *
     * @return The name associated with this MessageContext.
     */
    @Override
    public String getName() {
        return self.getName();
    }

    @Override
    public boolean unregister(String actorName) {
        return actorSystem.unregisterActor(actorName);
    }
}
