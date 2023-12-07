package paxel.lintstone.impl;

import java.util.function.BiConsumer;

/**
 * The {@code MessageContextFactory} class represents a factory for creating {@link MessageContext} objects.
 * It provides a method to construct an immutable {@link MessageContext} using the given parameters.
 */
public class MessageContextFactory {
    private final ActorSystem actorSystem;
    private final SelfUpdatingActorAccessor self;

    /**
     * The {@code MessageContextFactory} class represents a factory for creating {@link MessageContext} objects.
     * It provides a method to construct an immutable {@link MessageContext} using the given parameters.
     */
    public MessageContextFactory(ActorSystem actorSystem, SelfUpdatingActorAccessor self) {
        this.actorSystem = actorSystem;
        this.self = self;
    }

    /**
     * Creates a new {@link MessageContext} object with the given message and reply handler.
     *
     * @param message      The message to be processed by the actor system.
     * @param replyHandler The handler to be invoked when a reply is received.
     * @return The newly created MessageContext object.
     */
    public MessageContext create(Object message, BiConsumer<Object, SelfUpdatingActorAccessor> replyHandler) {
        return new MessageContext(message, actorSystem, self, replyHandler);
    }
}
