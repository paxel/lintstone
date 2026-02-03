package paxel.lintstone.impl;

import paxel.lintstone.api.LintStoneEventHandler;
import paxel.lintstone.api.LintStoneMessageEventContext;

/**
 * Interface for the message dispatch logic.
 */
interface DecisionTree {
    /**
     * Dispatches the message to the appropriate handler.
     *
     * @param message the message to handle.
     * @param context the context to pass to the handler.
     */
    void handle(Object message, LintStoneMessageEventContext context);
}

/**
 * Optimized implementation of {@link DecisionTree} using an array of handlers.
 */
class StaticDecisionTree implements DecisionTree {
    private final HandlerEntry<?>[] handlers;
    private final LintStoneEventHandler<Object> otherwiseHandler;

    StaticDecisionTree(HandlerEntry<?>[] handlers, LintStoneEventHandler<Object> otherwiseHandler) {
        this.handlers = handlers;
        this.otherwiseHandler = otherwiseHandler;
    }

    @Override
    public void handle(Object message, LintStoneMessageEventContext context) {
        for (HandlerEntry<?> entry : handlers) {
            if (entry.clazz().isInstance(message)) {
                entry.handle(message, context);
                return;
            }
        }
        if (otherwiseHandler != null) {
            otherwiseHandler.handle(message, context);
        }
    }

    /**
     * A single entry in the decision tree.
     */
    record HandlerEntry<T>(Class<T> clazz, LintStoneEventHandler<T> handler) {
        void handle(Object message, LintStoneMessageEventContext context) {
            handler.handle(clazz.cast(message), context);
        }
    }
}
