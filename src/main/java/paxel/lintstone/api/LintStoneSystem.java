package paxel.lintstone.api;

import java.time.Duration;
import java.util.Optional;

/**
 * The LintStone Actor system.
 */
public interface LintStoneSystem {


    /**
     * This generates and registers an Actor according to the given {@link ActorSettings}.
     *
     * @param name        The name of the actor. The name must be unique in the system.
     * @param factory     The factory to create the actor if not already exists.
     * @param initMessage The optional init message.
     * @param settings    The actor settings. Use {@link ActorSettings#create()} to create a builder and {@link ActorSettingsBuilder#build()} to build the instance.
     * @return The {@link LintStoneActorAccess} object
     */
    LintStoneActorAccess registerActor(String name, LintStoneActorFactory factory, Optional<Object> initMessage, ActorSettings settings);

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
     * @return {@code true} if shutdown happened before the timeout.
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
