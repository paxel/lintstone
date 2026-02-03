package paxel.lintstone.impl;

import lombok.NonNull;
import paxel.lintstone.api.LintStoneEventHandler;
import paxel.lintstone.api.LintStoneMessageEventContext;
import paxel.lintstone.api.MessageAccess;

/**
 * Implementation of {@link MessageAccess} for dynamic message matching.
 */
public class DynamicMessageAccess implements MessageAccess {

    private @NonNull Object message;
    private @NonNull LintStoneMessageEventContext context;
    private boolean handled;

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

    @Override
    public <T> @NonNull MessageAccess inCase(@NonNull Class<T> clazz, @NonNull LintStoneEventHandler<T> lintStoneEventHandler) {
        if (!handled && clazz.isInstance(message)) {
            handled = true;
            lintStoneEventHandler.handle(clazz.cast(message), context);
        }
        return this;
    }

    @Override
    public void otherwise(@NonNull LintStoneEventHandler<Object> catchAll) {
        if (!handled) {
            catchAll.handle(message, context);
        }
    }
}
