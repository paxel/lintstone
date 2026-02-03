package paxel.lintstone.api;

/**
 * Functional interface for handling errors in an actor.
 */
@FunctionalInterface
public interface ErrorHandler {

    /**
     * This Errorhandler is called whenever an Exception is caused by an Actor.
     * The handler decides if the Actor should continue {@link ErrorHandlerDecision#CONTINUE} or
     * abort processing {@link ErrorHandlerDecision#ABORT}.
     *
     * @param exception the Exception caught by the {@link paxel.lintstone.impl.SequentialProcessor}
     * @return The decision of the ErrorHandler
     */
    ErrorHandlerDecision handleError(Exception exception);

}
