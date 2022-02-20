package paxel.lintstone.impl;

import java.util.function.BiConsumer;

/**
 * Constructs MessageContexts for dedicated calls, that stay valid forever.
 * This is needed to use the ask context inside the reply handler.
 */
public class MessageContextFactory {
    private final ActorSystem actorSystem;
    private final SelfUpdatingActorAccess self;

    /**
     * The factory is created with the actorSystem and the current actor access.
     *
     * @param actorSystem The system.
     * @param self        the current actor access.
     */
    public MessageContextFactory(ActorSystem actorSystem, SelfUpdatingActorAccess self) {
        this.actorSystem = actorSystem;
        this.self = self;
    }

    /**
     * Constructs an immutable MessageContext.
     *
     * @param message      The message of the context.
     * @param replyHandler The reply handler for the reply method of the context.
     * @return The MessageContext.
     */
    public MessageContext create(Object message, BiConsumer<Object, SelfUpdatingActorAccess> replyHandler) {
        return new MessageContext(message, actorSystem, self, replyHandler);
    }
}
