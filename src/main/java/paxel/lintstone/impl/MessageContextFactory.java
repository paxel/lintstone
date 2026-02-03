package paxel.lintstone.impl;

import lombok.NonNull;

import java.util.function.BiConsumer;

/**
 * Constructs MessageContexts for dedicated calls, that stay valid forever.
 * This is needed to use the ask context inside the reply handler.
 */
public class MessageContextFactory {
    private final @NonNull ActorSystem actorSystem;
    private final @NonNull SelfUpdatingActorAccessor self;

    /**
     * The factory is created with the actorSystem and the current actor access.
     *
     * @param actorSystem The system.
     * @param self        the current actor access.
     */
    public MessageContextFactory(@NonNull ActorSystem actorSystem, @NonNull SelfUpdatingActorAccessor self) {
        this.actorSystem = actorSystem;
        this.self = self;
    }

    /**
     * Reuses the immutable MessageContext.
     *
     * @param message      The message of the context.
     * @param replyHandler The reply handler for the reply method of the context.
     * @return The MessageContext.
     */
    public @NonNull MessageContext create(@NonNull Object message, @NonNull BiConsumer<Object, SelfUpdatingActorAccessor> replyHandler) {
        MessageContext context = createContext();
        context.reset(message, replyHandler);
        return context;
    }

    /**
     * Creates a new MessageContext.
     *
     * @return The MessageContext.
     */
    public @NonNull MessageContext createContext() {
        return new MessageContext(actorSystem, self);
    }
}
