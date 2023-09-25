package paxel.lintstone.api;

/**
 * This interface defines a method that handles an event type. An actor should
 * implement it for each message type it wants to process and delegate the
 * function to the context. The context is given to the function so that the
 * context must not be stored as member.
 * <p>
 * Basically this is our way to make typesafe calls without instanceof switches
 * or if else trees
 */
@FunctionalInterface
public interface LintStoneEventHandler<T> {

    /**
     * Handles a message event of the given context.
     *
     * @param eventValue the value in correct type
     * @param context the context
     */
    void handle(T eventValue, LintStoneMessageEventContext context);
}
