package paxel.lintstone.api;

/**
 * Is thrown in case a message is sent to an actor who is not registered.
 */
public class UnregisteredRecipientException extends RuntimeException {

    /**
     * Constructs a new exception with the given message.
     *
     * @param message the exception message.
     */
    public UnregisteredRecipientException(String message) {
        super(message);
    }

}
