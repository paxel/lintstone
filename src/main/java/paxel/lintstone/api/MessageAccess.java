package paxel.lintstone.api;

import lombok.NonNull;

/**
 * This interface provides access to the message or the reply.
 */
public interface MessageAccess {

    /**
     * If the given class is assignable from the message class, then the event handler will be called.
     *
     * @param clazz                 The class
     * @param lintStoneEventHandler The handler
     * @param <T>                   The type.
     * @return A Monad.
     */
    <T> @NonNull MessageAccess inCase(@NonNull Class<T> clazz, @NonNull LintStoneEventHandler<T> lintStoneEventHandler);

    /**
     * This will execute the catchAll only if no match happened before. This only works if the calls are chained.
     *
     * @param catchAll The handler for unknown types.
     */
    void otherwise(@NonNull LintStoneEventHandler<Object> catchAll);
}
