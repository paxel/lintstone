package paxel.lintstone.api;

/**
 * This interface is used to send messages to an actor. The send message
 *
 * @author axel
 */
public interface LintStoneActorAccess {

    /**
     * Sends a message to the Actor represented by this Access.
     *
     * @param message The message to send-
     * @throws UnregisteredRecipientException in case the actor does not exist.
     */
    void send(Object message) throws UnregisteredRecipientException;

    /**
     * Retrieve if the actor is currently registered.
     *
     * @return {@code true if the actor is registered and can receive a message}
     */
    boolean exists();

}
