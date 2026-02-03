package paxel.lintstone.api;

import lombok.NonNull;
import paxel.lintstone.impl.ActorSettingsBuilder;

import java.time.Duration;

/**
 * The LintStone Actor system.
 */
public interface LintStoneSystem {


    /**
     * This generates and registers an Actor according to the given {@link ActorSettings}.
     *
     * @param name        The name of the actor. The name must be unique in the system.
     * @param factory     The factory to create the actor if not already exists.
     * @param settings    The actor settings. Use {@link ActorSettings#create()} to create a builder and {@link ActorSettingsBuilder#build()} to build the instance.
     * @param initMessage The init message. Can be null, but you should use {@link #registerActor(String, LintStoneActorFactory, ActorSettings)} then instead
     * @return The {@link LintStoneActorAccessor} object
     */
    LintStoneActorAccessor registerActor(@NonNull String name, @NonNull LintStoneActorFactory factory, @NonNull ActorSettings settings, Object initMessage);

    /**
     * This generates and registers an Actor according to the given {@link ActorSettings}.
     *
     * @param name     The name of the actor. The name must be unique in the system.
     * @param factory  The factory to create the actor if not already exists.
     * @param settings The actor settings. Use {@link ActorSettings#create()} to create a builder and {@link ActorSettingsBuilder#build()} to build the instance.
     * @return The {@link LintStoneActorAccessor} object
     */
    LintStoneActorAccessor registerActor(@NonNull String name, @NonNull LintStoneActorFactory factory, @NonNull ActorSettings settings);

    /**
     * This retrieves an{@link LintStoneActorAccessor} for the given name.
     *
     * @param name The name of the actor. The name must be unique in the system.
     * @return The {@link LintStoneActorAccessor} object
     */
    LintStoneActorAccessor getActor(@NonNull String name);

    /**
     * This will stop the executor in the system after all messages are
     * processed. The method returns immediately. That does not mean, that all
     * messages are processed on return.
     */
    void shutDown();

    /**
     * This will tell all executors to stop when idle.
     * The method returns when all Actors have stopped.
     * This method does not unregister the actors.
     * The actors also accept new messages during shutdown
     *
     * @throws java.lang.InterruptedException in case the Thread is interrupted while shutting down.
     */
    void shutDownAndWait() throws InterruptedException;

    /**
     * This will tell all executors to stop when idle. If all Actors have stopped before timeout, it returns early with true.
     * Otherwise, after given duration with false.
     * This method does not unregister the actors.
     * The actors also accept new messages during shutdown
     *
     * @param timeout the duration to wait
     * @return {@code true} if all actors have stopped before timeout, {@code false} otherwise.
     * @throws java.lang.InterruptedException in case the Thread is interrupted while shutting down.
     */
    boolean shutDownAndWait(@NonNull Duration timeout) throws InterruptedException;

    /**
     * This will tell all executors to stop immediately.
     * This method unregisters the actors.
     * The actors do not accept new messages during shutdown.
     * Unprocessed messages are ignored.
     */
    void shutDownNow();

    /**
     * This will remove an actor from the system. All messages queued before
     * this call are still processed. Messages that are queued to this actor
     * after this call will fail.
     *
     * @param name The actor to be removed.
     * @return {@code true} if the actor existed and was removed.
     */
    boolean unregisterActor(@NonNull String name);
}
