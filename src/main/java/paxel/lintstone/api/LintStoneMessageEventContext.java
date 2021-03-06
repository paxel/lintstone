package paxel.lintstone.api;

import java.util.Optional;

/**
 * Represents the access to the message and the actor system for one message
 * event.
 */
public interface LintStoneMessageEventContext {

    /**
     * The context executes the given consumer with the message object, if it is
     * an instance of the given class. Otherwise nothing happens. If any inCase
     * method matched, no other inCase method nor the otherwise method will be
     * executed.
     * <p>
     * {@code mce.inCase(String.class, s-> println(s))}<br>
     * {@code    .inCase(Integer.class, this::add)}<br>
     * {@code    .otherWise(o->LOG.warning("Unknown type "+o.getClass());}<br>
     * <p>
     * will either print the string, add the int or log the warning.
     *
     * @param <T>      The type of the class.
     * @param clazz    The class
     * @param consumer The consumer of messages of the class.
     * @return the context itself
     */
    <T> LintStoneMessageEventContext inCase(Class<T> clazz, LintStoneEventHandler<T> consumer);

    /**
     * Is executed if no
     * {@link #inCase(java.lang.Class, paxel.lintstone.api.LintStoneEventHandler) }
     * was successfully executed before
     *
     * @param message The message
     */
    void otherwise(LintStoneEventHandler<Object> message);

    /**
     * Replies to the sender of the message. If the sender is no actor, the
     * method throws a NoSenderException, that can be called by the Actor, or is
     * handled by the LintStoneSystem.
     *
     * @param msg the message to reply.
     * @throws UnregisteredRecipientException if there is no actor with that
     *                                        name.
     * @throws NoSenderException              if there is no sender for the current message.
     */
    void reply(Object msg) throws NoSenderException, UnregisteredRecipientException;

    /**
     * Sends the message to the actor with the registered name.
     *
     * @param name the name of the actor.
     * @param msg  The message to send.
     * @throws UnregisteredRecipientException if there is no actor with that
     *                                        name.
     */
    void send(String name, Object msg) throws UnregisteredRecipientException;

    /**
     * Retrieve the actor with given name. This method will always return an
     * object. Use the provided object to check if the actor exists by calling {@link  LintStoneActorAccess#exists()
     * }. Please note, that the existance of the actor might change, and thus
     * the result of exists might change later.
     *
     * @param name The name of the actor.
     * @return The actor access.
     */
    LintStoneActorAccess getActor(String name);

    /**
     * This method deleagtes to
     * {@link LintStoneSystem#registerActor(String, LintStoneActorFactory, Optional)}.
     *
     * @param name        The name of the actor.
     * @param factory     The factory.
     * @param initMessage The init message.
     * @return The new or old actor access.
     */
    LintStoneActorAccess registerActor(String name, LintStoneActorFactory factory, Optional<Object> initMessage);

    /**
     * This method deleagtes to
     * {@link LintStoneSystem#registerSingleSourceActor(String, LintStoneActorFactory, Optional)}.
     *
     * @param name        The name of the actor.
     * @param factory     The factory.
     * @param initMessage The init message.
     * @return The new or old actor access.
     */
    LintStoneActorAccess registerSingleSourceActor(String name, LintStoneActorFactory factory, Optional<Object> initMessage);

    /**
     * This method deleagtes to
     * {@link LintStoneSystem#registerMultiSourceActor(java.lang.String, paxel.lintstone.api.LintStoneActorFactory, java.util.Optional)}.
     *
     * @param name        The name of the actor.
     * @param factory     The factory.
     * @param initMessage The init message.
     * @return The new or old actor access.
     */
    LintStoneActorAccess registerMultiSourceActor(String name, LintStoneActorFactory factory, Optional<Object> initMessage);

    /**
     * Unregisters this Actor.
     *
     * @return {@code true} if the actor was removed.
     */
    boolean unregister();

    /**
     * Retrieve the name of this actor.
     *
     * @return the name
     */
    public String getName();

    /**
     * Unregister some actor.
     *
     * @param actorName The actor to be unregistered.
     * @return {@code true} if the actor was unregistered
     */
    boolean unregister(String actorName);

}
