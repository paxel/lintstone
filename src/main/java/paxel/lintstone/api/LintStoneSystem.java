package paxel.lintstone.api;

import paxel.lintstone.impl.ActorSettingsBuilder;
import paxel.lintstone.impl.SelfUpdatingActorAccess;

import java.time.Duration;
import java.util.Optional;

/**
 * The LintStone Actor system.
 */
public interface LintStoneSystem {

    /**
     * This checks if there is already an Actor with the given name, and if not
     * uses the {@link LintStoneActorFactory} to create one. The initMessage
     * will be the first message the newly created actor will receive.
     *
     * @param name        The name of the actor. The name must be unique in the system.
     * @param factory     The factory to create the actor if not already exists.
     * @param initMessage The optional init message.
     * @return The {@link LintStoneActorAccess} object
     */
    @Deprecated
    LintStoneActorAccess registerMultiSourceActor(String name, LintStoneActorFactory factory, Optional<Object> initMessage);

    /**
     * This is the same method as {@link #registerMultiSourceActor(String, LintStoneActorFactory, Optional)}
     *
     * @param name        The name of the actor. The name must be unique in the system.
     * @param factory     The factory to create the actor if not already exists.
     * @param initMessage The optional init message.
     * @return The {@link LintStoneActorAccess} object
     */
    @Deprecated
    LintStoneActorAccess registerActor(String name, LintStoneActorFactory factory, Optional<Object> initMessage);

    /**
     * This checks if there is already an Actor with the given name, and if not
     * uses the {@link LintStoneActorFactory} to create one. The initMessage
     * will be the first message the newly created actor will receive.
     * <p>
     * The resulting actor is a bit better performing than the one resulting from {@link #registerMultiSourceActor(String, LintStoneActorFactory, Optional)},
     * but can only receive messages from a single thread.
     *
     * @param name        The name of the actor. The name must be unique in the system.
     * @param factory     The factory to create the actor if not already exists.
     * @param initMessage The optional init message.
     * @return The {@link LintStoneActorAccess} object
     */
    @Deprecated
    LintStoneActorAccess registerSingleSourceActor(String name, LintStoneActorFactory factory, Optional<Object> initMessage);

    /**
     * This generates and registers an Actor according to the given {@link ActorSettings}.
     *
     * @param name        The name of the actor. The name must be unique in the system.
     * @param factory     The factory to create the actor if not already exists.
     * @param initMessage The optional init message.
     * @param settings The actor settings. Use {@link ActorSettings#create()} to create a builder and {@link ActorSettingsBuilder#build()} to build the instance.
     * @return The {@link LintStoneActorAccess} object
     */
    public LintStoneActorAccess registerActor(String name, LintStoneActorFactory factory, Optional<Object> initMessage, ActorSettings settings);
    /**
     * This will stop the executor in the system after all messages are
     * processed. The method returns immediately. That does not mean, that all
     * messages are processed on return.
     */
    void shutDown();

    /**
     * This will stop the executor in the system after all messages are
     * processed.The method returns when all messsages are processed.
     *
     * @throws java.lang.InterruptedException in case the Thread is interrupted
     *                                        while shutting down.
     */
    void shutDownAndWait() throws InterruptedException;

    /**
     * This will stop the executor in the system after all messages are
     * processed.The method returns when all messsages are processed or the
     * timeout duration has passed.
     *
     * @param timeout the duration to wait
     * @return {@code false} if the method returned because timeout.
     * @throws java.lang.InterruptedException in case the Thread is interrupted
     *                                        while shutting down.
     */
    boolean shutDownAndWait(Duration timeout) throws InterruptedException;

    /**
     * This immediately kills the executor.
     */
    void shutDownNow();

    /**
     * This will remove an actor from the system. All messages queued before
     * this call are still processed. Messages that are queued to this actor
     * after this call will either fail or be sent to a new actor (if someone
     * created it)
     *
     * @param name The actor to be removed.
     * @return {@code true} if the actor existed and was removed.
     */
    boolean unregisterActor(String name);
}
