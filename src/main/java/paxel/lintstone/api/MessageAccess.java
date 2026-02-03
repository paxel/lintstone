package paxel.lintstone.api;

import lombok.NonNull;

/**
 * This class provides access to the message or the reply.
 */
public class MessageAccess {

    private @NonNull Object message;
    private @NonNull LintStoneMessageEventContext context;
    private boolean handled;

    /**
     * Default constructor.
     */
    public MessageAccess() {
    }

    /**
     * Resets the message access with a new message and context.
     *
     * @param message the message to handle.
     * @param context the context to use.
     */
    public void reset(@NonNull Object message, @NonNull LintStoneMessageEventContext context) {
        this.message = message;
        this.context = context;
        this.handled = false;
    }

    /**
     * If the given class is assignable from the message class, then the event handler will be called.
     *
     * @param clazz                 The class
     * @param lintStoneEventHandler The handler
     * @param <T>                   The type.
     * @return A Monad.
     */
    public <T> @NonNull MessageAccess inCase(@NonNull Class<T> clazz, @NonNull LintStoneEventHandler<T> lintStoneEventHandler) {
        if (!handled && clazz.isAssignableFrom(message.getClass())) {
            handled = true;
            lintStoneEventHandler.handle(clazz.cast(message), context);
        }
        return this;
    }

    /**
     * This will execute the catchAll only if no match happened before. This only works if the calls are chained.
     *
     * @param catchAll The handler for unknown types.
     */
    public void otherwise(@NonNull LintStoneEventHandler<Object> catchAll) {
        if (!handled) {
            catchAll.handle(message, context);
        }
    }
}
