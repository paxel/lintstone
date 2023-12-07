package paxel.lintstone.api;

/**
 * ErrorHandler is a functional interface for handling errors.
 * The handleError method is called to handle an error.
 */
@FunctionalInterface
public interface ErrorHandler {

    /**
     * Handles an error.
     *
     * @param failedMessage The object representing the error.
     * @return True if the error was handled successfully, false otherwise.
     */
    boolean handleError(Object failedMessage);
}
