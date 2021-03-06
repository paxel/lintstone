package paxel.lintstone.api;

/**
 * This interface is used to send messages to an actor. This object should never
 * be used multithreaded unless synchronized externally.
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
     * Retrieve if the actor is currently registered. using this does not ensure
     * that send will work, depending on how you register and unregister Actors.
     * If you unregister the actors during runtime of the system, they might
     * become unregistered after exists is called. It is recommended to register
     * actors on demand, and unregister them if it is absolutely clear that
     * nobody will call them again. If your design requires to unregister actors
     * randomly you should reconsider your design and ignore exists() and
     * directly send and catch the {@link UnregisteredRecipientException}
     *
     * @return {@code true if the actor is registered and can receive a message}
     */
    boolean exists();

}
