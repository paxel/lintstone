package paxel.lintstone.impl;

import lombok.NonNull;
import paxel.lintstone.api.*;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * Message context instance dedicated for a process of a message or reply
 */
public class MessageContext implements LintStoneMessageEventContext {

    private final @NonNull ActorSystem actorSystem;
    private final @NonNull SelfUpdatingActorAccessor self;
    private final @NonNull DynamicMessageAccess messageAccess = new DynamicMessageAccess();
    private final @NonNull DecisionTreeBuilder decisionTreeBuilder = new DecisionTreeBuilder();
    private @NonNull Object message;
    private @NonNull BiConsumer<Object, SelfUpdatingActorAccessor> replyHandler;
    private boolean recording = false;

    /**
     * Creates a new message context.
     *
     * @param actorSystem the actor system.
     * @param self        the actor accessor for this context.
     */
    public MessageContext(@NonNull ActorSystem actorSystem, @NonNull SelfUpdatingActorAccessor self) {
        this.actorSystem = actorSystem;
        this.self = self;
    }

    /**
     * Resets the context for a new message and reply handler.
     *
     * @param message      the message.
     * @param replyHandler the reply handler.
     */
    public void reset(@NonNull Object message, @NonNull BiConsumer<Object, SelfUpdatingActorAccessor> replyHandler) {
        this.message = message;
        this.replyHandler = replyHandler;
        this.messageAccess.reset(message, this);
    }

    @Override
    public <T> @NonNull MessageAccess inCase(@NonNull Class<T> clazz, @NonNull LintStoneEventHandler<T> consumer) {
        if (recording) {
            return decisionTreeBuilder.inCase(clazz, consumer);
        }
        return messageAccess.inCase(clazz, consumer);
    }

    @Override
    public void otherwise(@NonNull LintStoneEventHandler<Object> catchAll) {
        if (recording) {
            decisionTreeBuilder.otherwise(catchAll);
            return;
        }
        messageAccess.otherwise(catchAll);
    }

    @Override
    public void reply(@NonNull Object msg) throws NoSenderException, UnregisteredRecipientException {
        replyHandler.accept(msg, self);
    }

    @Override
    public void tell(@NonNull String name, @NonNull Object msg) throws UnregisteredRecipientException {
        Optional<Actor> actor = actorSystem.getOptionalActor(name);
        if (actor.isEmpty()) {
            throw new UnregisteredRecipientException("Actor with name " + name + " does not exist");
        }
        actor.get().send(msg, self, null);
    }

    @Override
    public void tell(@NonNull String name, @NonNull Object msg, @NonNull Duration delay) throws UnregisteredRecipientException {
        Optional<Actor> actor = actorSystem.getOptionalActor(name);
        if (actor.isEmpty()) {
            throw new UnregisteredRecipientException("Actor with name " + name + " does not exist");
        }
        actor.get().send(msg, self, null, delay);
    }

    @Override
    public void ask(@NonNull String name, @NonNull Object msg, @NonNull ReplyHandler handler) throws UnregisteredRecipientException {
        Optional<Actor> actor = actorSystem.getOptionalActor(name);
        if (actor.isEmpty()) {
            throw new UnregisteredRecipientException("Actor with name " + name + " does not exist");
        }
        actor.get().send(msg, self, handler);
    }

    @Override
    public <F> @NonNull CompletableFuture<F> ask(@NonNull String name, @NonNull Object msg) throws UnregisteredRecipientException {
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
    public @NonNull LintStoneActorAccessor getActor(@NonNull String name) {
        // give an empty ref, that is filled on demand.
        return new SelfUpdatingActorAccessor(name, null, actorSystem, self);
    }


    @Override
    public @NonNull LintStoneActorAccessor registerActor(@NonNull String name, @NonNull LintStoneActorFactory factory, Object initMessage, @NonNull ActorSettings settings) {
        return actorSystem.registerActor(name, factory, self, settings, initMessage);
    }

    @Override
    public @NonNull LintStoneActorAccessor registerActor(@NonNull String name, @NonNull LintStoneActorFactory factory, @NonNull ActorSettings settings) {
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
    public boolean unregister(@NonNull String actorName) {
        return actorSystem.unregisterActor(actorName);
    }

    void setRecording(boolean recording) {
        this.recording = recording;
    }

    DecisionTree getDecisionTree() {
        return decisionTreeBuilder.build();
    }
}
