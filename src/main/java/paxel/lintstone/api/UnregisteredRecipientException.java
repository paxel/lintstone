package paxel.lintstone.api;

/**
 * Is thrown in case a message is sent to a actor who is not registered.
 */
public class UnregisteredRecipientException extends RuntimeException {

    public UnregisteredRecipientException(String message) {
        super(message);
    }

}
