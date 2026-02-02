package paxel.lintstone.api;

/**
 * This class provides access to the message or the reply.
 */
public class MessageAccess {

    private Object message;
    private LintStoneMessageEventContext context;
    private boolean handled;

    public MessageAccess() {
    }

    public void reset(Object message, LintStoneMessageEventContext context) {
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
    public <T> MessageAccess inCase(Class<T> clazz, LintStoneEventHandler<T> lintStoneEventHandler) {
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
    public void otherwise(LintStoneEventHandler<Object> catchAll) {
        if (!handled) {
            catchAll.handle(message, context);
        }
    }
}
