package paxel.lintstone.impl;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import paxel.lintstone.api.*;

/**
 * This class represents an Actor System in the LintStone framework.
 */
public class ActorSystem implements LintStoneSystem {

    private final Map<String, Actor> actors = Collections.synchronizedMap(new HashMap<>());
    private final GroupingExecutor groupingExecutor;

    /**
     * The ActorSystem class represents the LintStone Actor system.
     */
    public ActorSystem() {
        groupingExecutor = new GroupingExecutor();
    }

    /**
     * Registers an actor in the system with the specified name, factory, settings, and optional init message.
     *
     * @param name        The name of the actor. The name must be unique in the system.
     * @param factory     The factory to create the actor if not already exists.
     * @param settings    The actor settings. Use {@link ActorSettings#create()} to create a builder and {@link ActorSettingsBuilder#build()} to build the instance.
     * @param initMessage The init message. Can be null, but you should use {@link #registerActor(String, LintStoneActorFactory, ActorSettings)} then instead.
     * @return The {@link LintStoneActorAccessor} for the registered actor.
     */
    @Override
    public LintStoneActorAccessor registerActor(String name, LintStoneActorFactory factory, ActorSettings settings, Object initMessage) {
        return registerActor(name, factory, null, settings, initMessage);
    }

    /**
     * Registers an actor in the system with the specified name, factory, and settings.
     *
     * @param name     The name of the actor. The name must be unique in the system.
     * @param factory  The factory to create the actor if it does not already exist.
     * @param settings The actor settings.
     * @return The LintStoneActorAccessor for the registered actor.
     */
    @Override
    public LintStoneActorAccessor registerActor(String name, LintStoneActorFactory factory, ActorSettings settings) {
        return registerActor(name, factory, null, settings, null);
    }

    /**
     * Retrieves the LintStoneActorAccessor for the actor with the specified name.
     *
     * @param name The name of the actor. The name must be unique in the system.
     * @return The LintStoneActorAccessor for the actor with the specified name.
     */
    @Override
    public LintStoneActorAccessor getActor(String name) {
        synchronized (actors) {
            return new SelfUpdatingActorAccessor(name, actors.get(name), this, null);
        }
    }

    /**
     * Registers an actor in the system with the specified name, factory, sender, settings, and optional init message.
     *
     * @param name        The name of the actor. The name must be unique in the system.
     * @param factory     The factory to create the actor if not already exists.
     * @param sender      The sender of the actor.
     * @param settings    The actor settings. Use {@link ActorSettings#create()} to create a builder and {@link ActorSettingsBuilder#build()} to build the instance.
     * @param initMessage The init message. Can be null, but you should use {@link #registerActor(String, LintStoneActorFactory, ActorSettings)} then instead.
     * @return The {@link LintStoneActorAccessor} for the registered actor.
     */
    LintStoneActorAccessor registerActor(String name, LintStoneActorFactory factory, SelfUpdatingActorAccessor sender, ActorSettings settings, Object initMessage) {
        SequentialProcessorBuilder sequentialProcessorBuilder = groupingExecutor.create();
        sequentialProcessorBuilder.setErrorHandler(settings.errorHandler());
        return registerActor(name, factory, initMessage, sender, sequentialProcessorBuilder);
    }


    /**
     * Registers an actor in the system with the specified name, factory, initMessage, sender, and sequentialProcessor.
     *
     * @param name                   The name of the actor. The name must be unique in the system.
     * @param factory                The factory to create the actor if not already exists.
     * @param initMessage            The initialization message for the actor. Can be null.
     * @param sender                 The sender of the actor.
     * @param sequentialProcessor    The sequential processor for the actor.
     * @return The LintStoneActorAccessor for the registered actor.
     */
    private LintStoneActorAccessor registerActor(String name, LintStoneActorFactory factory, Object initMessage, SelfUpdatingActorAccessor sender, SequentialProcessorBuilder sequentialProcessor) {
        synchronized (actors) {
            Actor existing = actors.get(name);
            if (existing != null) {
                return new SelfUpdatingActorAccessor(name, existing, this, sender);
            }
            LintStoneActor actorInstance = factory.create();
            Actor newActor = new Actor(name, actorInstance, sequentialProcessor.build(), this, sender);
            // actor receives the initMessage as first message.
            Optional.ofNullable(initMessage).ifPresent(msg -> newActor.send(msg, null, null, null));
            actors.put(name, newActor);
            return new SelfUpdatingActorAccessor(name, newActor, this, sender);
        }
    }


    /**
     * Shuts down the Actor system.
     *
     * This method will stop all Actors in the system after all messages are processed.
     * The method returns immediately, but it does not guarantee that all messages are processed on return.
     *
     * This method is called internally to gracefully shut down the system.
     * It first calls the private method shutdownActors with a parameter "false" to shutdown all Actors in the system.
     * Then it calls the shutdown method of the groupingExecutor to shut down the executor service used by the system.
     *
     * @see ActorSystem#shutdownActors(boolean)
     * @see LintStoneSystem#shutDown()
     */
    @Override
    public void shutDown() {
        shutdownActors(false);
        groupingExecutor.shutdown();
    }

    /**
     * Shuts down the ActorSystem and waits for all Actors to complete processing their messages.
     *
     * @throws InterruptedException If the thread is interrupted while waiting for termination.
     */
    @Override
    public void shutDownAndWait() throws InterruptedException {
        shutdownActors(false);
        groupingExecutor.shutdown();
        //wait forever and a day
        groupingExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.HOURS);
    }

    /**
     * Shuts down the ActorSystem and waits for all Actors to complete processing their messages.
     *
     * @param timeout the duration to wait
     * @return true if all Actors have terminated within the specified timeout, false otherwise
     * @throws InterruptedException if the thread is interrupted while waiting for termination
     */
    @Override
    public boolean shutDownAndWait(Duration timeout) throws InterruptedException {
        shutdownActors(false);
        groupingExecutor.shutdown();
        return groupingExecutor.awaitTermination(timeout.getSeconds(), TimeUnit.SECONDS);
    }

    /**
     * Shuts down the ActorSystem immediately.
     *
     * This method shuts down all Actors in the system and terminates the executor service used by the system.
     *
     * @see ActorSystem#shutdownActors(boolean)
     * @see ActorSystem#groupingExecutor
     */
    @Override
    public void shutDownNow() {
        shutdownActors(true);
        groupingExecutor.shutdownNow();
    }

    /**
     * Shuts down the actors in the system.
     *
     * This method is called internally to gracefully shut down the system.
     * It iterates over all the actors in the system and calls their shutdown method.
     * The shutdown of each actor can be immediate or graceful depending on the value of the "now" parameter.
     *
     * @param now true if the shutdown should be immediate, false if it should be graceful
     */
    private void shutdownActors(boolean now) {
        synchronized (actors) {
            actors.values().stream().forEach(a -> a.shutdown(now));
        }
    }

    /**
     * Unregisters an actor from the system.
     *
     * @param name The name of the actor to be removed.
     * @return true if the actor was successfully unregistered, false otherwise.
     */
    @Override
    public boolean unregisterActor(String name) {
        synchronized (actors) {
            Actor remove = actors.remove(name);
            if (remove != null) {
                // this actor will not accept any messages anymore. The Accesses should try to get a new instance or fail.
                remove.unregisterGracefully();
                return true;
            }
            return false;
        }
    }

    /**
     * Retrieves an optional Actor object by name.
     *
     * @param name The name of the Actor.
     * @return An Optional<Actor> object representing the Actor with the specified name, or an empty Optional if the Actor does not exist.
     */
    Optional<Actor> getOptionalActor(String name) {
        synchronized (actors) {
            return Optional.ofNullable(actors.get(name));
        }
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("ActorSystem{");

        actors.forEach((a, f) -> stringBuilder.append(f.toString()).append("\n"));
        stringBuilder.append(" exec:").append(groupingExecutor.toString());
        stringBuilder.append("}");

        return stringBuilder.toString();
    }
}
