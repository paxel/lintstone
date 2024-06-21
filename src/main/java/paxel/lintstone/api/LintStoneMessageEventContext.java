package paxel.lintstone.api;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the access to the message and the actor system for one message
 * event.
 */
public interface LintStoneMessageEventContext {

    /**
     * The context executes the given consumer with the message object, if it is
     * an instance of the given class. Otherwise, nothing happens. If any inCase
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
    <T> TypeSafeMonad inCase(Class<T> clazz, LintStoneEventHandler<T> consumer);

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
    void tell(String name, Object msg) throws UnregisteredRecipientException;

    /**
     * Sends the message to the actor with the registered name.
     * The replies of that actor are processed by the given Reply Handler in the thread context of this actor.
     *
     * @param name    the name of the actor.
     * @param msg     The message to send.
     * @param handler The reply handler.
     * @throws UnregisteredRecipientException if there is no actor with that
     *                                        name.
     */
    void ask(String name, Object msg, ReplyHandler handler) throws UnregisteredRecipientException;

    /**
     * Sends the message to the actor with the registered name.
     * The first reply will complete the resulting future in the context of this actor.
     * If the replied type doesn't match the future it is completed exceptionally.
     *
     * @param name the name of the actor.
     * @param msg  The message to send.
     * @param <F>  the type of the future.
     * @return the future result.
     * @throws UnregisteredRecipientException if there is no actor with that
     *                                        name.
     */
    <F> CompletableFuture<F> ask(String name, Object msg) throws UnregisteredRecipientException;

    /**
     * Retrieve the actor with given name. This method will always return an
     * object. Use the provided object to check if the actor exists by calling {@link  LintStoneActorAccessor#exists()
     * }. Please note, that the existence of the actor might change, and thus
     * the result of exists might change later.
     *
     * @param name The name of the actor.
     * @return The actor access.
     */
    LintStoneActorAccessor getActor(String name);

    /**
     * This method delegates to
     * {@link LintStoneSystem#registerActor(String, LintStoneActorFactory, ActorSettings, Object)}.
     *
     * @param name        The name of the actor.
     * @param factory     The factory.
     * @param initMessage The init message.
     * @return The new or old actor access.
     */
    LintStoneActorAccessor registerActor(String name, LintStoneActorFactory factory, Object initMessage, ActorSettings settings);

    /**
     * This method delegates to
     * {@link LintStoneSystem#registerActor(String, LintStoneActorFactory, ActorSettings)}.
     *
     * @param name        The name of the actor.
     * @param factory     The factory.
     * @return The new or old actor access.
     */
    LintStoneActorAccessor registerActor(String name, LintStoneActorFactory factory, ActorSettings settings);

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
    String getName();

    /**
     * Unregister some actor.
     *
     * @param actorName The actor to be unregistered.
     * @return {@code true} if the actor was unregistered
     */
    boolean unregister(String actorName);

}
