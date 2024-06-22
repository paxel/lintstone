package paxel.lintstone.api;

/**
 * This class provides access to the message or the reply.
 */
public class MessageAccess {
    private static final MessageAccess DONE = new MessageAccess(null, null) {
        @Override
        public <T> MessageAccess inCase(Class<T> clazz, LintStoneEventHandler<T> lintStoneEventHandler) {
            return this;
        }

        @Override
        public void otherwise(LintStoneEventHandler<Object> catchAll) {
        }
    };

    private final Object message;
    private final LintStoneMessageEventContext context;

    public MessageAccess(Object message, LintStoneMessageEventContext context) {
        this.message = message;
        this.context = context;
    }

    /**
     * If the given class is assignable from the message class, then the event handler will be called.
     * The returned Monad will be non-functional. Otherwise, the Monad returns itself.
     *
     * @param clazz                 The class
     * @param lintStoneEventHandler The handler
     * @param <T>                   The type.
     * @return A Monad.
     */
    public <T> MessageAccess inCase(Class<T> clazz, LintStoneEventHandler<T> lintStoneEventHandler) {
        if (clazz.isAssignableFrom(message.getClass())) {
            lintStoneEventHandler.handle(clazz.cast(message), context);
            return DONE;
        }
        return this;
    }

    /**
     * This will execute the catchAll only if no match happened before. This only works if the calls are chained.
     *
     * @param catchAll The handler for unknown types.
     */
    public void otherwise(LintStoneEventHandler<Object> catchAll) {
        catchAll.handle(message, context);
    }
}
