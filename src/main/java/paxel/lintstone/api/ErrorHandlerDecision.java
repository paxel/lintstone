package paxel.lintstone.api;

/**
 * The decision of the {@link ErrorHandler} after a caught Exception.
 */
public enum ErrorHandlerDecision {
    /**
     * Continue processing messages.
     */
    CONTINUE,
    /**
     * Abort processing messages and stop the actor.
     */
    ABORT;
}
