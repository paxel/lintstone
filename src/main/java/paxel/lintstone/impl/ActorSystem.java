package paxel.lintstone.impl;

import paxel.lintstone.api.*;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Default implementation of {@link LintStoneSystem}.
 */
public class ActorSystem implements LintStoneSystem {

    private final Map<String, Actor> actors = new ConcurrentHashMap<>();
    private final ProcessorFactory processorFactory;
    private final Scheduler scheduler;
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Creates a new ActorSystem with default {@link GroupingExecutor} and {@link SimpleScheduler}.
     */
    public ActorSystem() {
        processorFactory = new GroupingExecutor();
        SimpleScheduler simpleScheduler = new SimpleScheduler();
        Thread.ofVirtual().start(simpleScheduler);
        scheduler = simpleScheduler;
    }

    /**
     * Creates a new ActorSystem with the given processor factory and scheduler.
     *
     * @param processorFactory the processor factory to use.
     * @param scheduler        the scheduler to use.
     */
    public ActorSystem(ProcessorFactory processorFactory, Scheduler scheduler) {
        this.processorFactory = processorFactory;
        this.scheduler = scheduler;
    }

    @Override
    public LintStoneActorAccessor registerActor(String name, LintStoneActorFactory factory, ActorSettings settings, Object initMessage) {
        return registerActor(name, factory, null, settings, initMessage);
    }

    @Override
    public LintStoneActorAccessor registerActor(String name, LintStoneActorFactory factory, ActorSettings settings) {
        return registerActor(name, factory, null, settings, null);
    }

    @Override
    public LintStoneActorAccessor getActor(String name) {
        return new SelfUpdatingActorAccessor(name, actors.get(name), this, null);
    }

    LintStoneActorAccessor registerActor(String name, LintStoneActorFactory factory, SelfUpdatingActorAccessor sender, ActorSettings settings, Object initMessage) {
        SequentialProcessorBuilder sequentialProcessorBuilder = processorFactory.create();
        sequentialProcessorBuilder.setErrorHandler(settings.errorHandler());
        return registerActor(name, factory, initMessage, sender, sequentialProcessorBuilder);
    }


    private LintStoneActorAccessor registerActor(String name, LintStoneActorFactory factory, Object initMessage, SelfUpdatingActorAccessor sender, SequentialProcessorBuilder sequentialProcessor) {
        try (AutoClosableLock ignored = new AutoClosableLock(lock)) {
            Actor existing = actors.get(name);
            if (existing != null) {
                return new SelfUpdatingActorAccessor(name, existing, this, sender);
            }
            LintStoneActor actorInstance = factory.create();
            Actor newActor = new Actor(name, actorInstance, sequentialProcessor.build(), this, sender, scheduler);
            // actor receives the initMessage as first message.
            Optional.ofNullable(initMessage).ifPresent(msg -> newActor.send(msg, null, null));
            actors.put(name, newActor);
            return new SelfUpdatingActorAccessor(name, newActor, this, sender);
        }
    }


    @Override
    public void shutDown() {
        shutdownActors(false);
        processorFactory.shutdown();
        scheduler.shutDown();
    }

    @Override
    public void shutDownAndWait() throws InterruptedException {
        shutdownActors(false);
        processorFactory.shutdown();
        scheduler.shutDown();
        //wait forever and a day
        processorFactory.awaitTermination(Long.MAX_VALUE, TimeUnit.HOURS);
    }

    @Override
    public boolean shutDownAndWait(Duration timeout) throws InterruptedException {
        shutdownActors(false);
        processorFactory.shutdown();
        scheduler.shutDown();
        return processorFactory.awaitTermination(timeout.getSeconds(), TimeUnit.SECONDS);
    }

    @Override
    public void shutDownNow() {
        shutdownActors(true);
        processorFactory.shutdownNow();
        scheduler.shutDown();
    }

    private void shutdownActors(boolean now) {
        try (AutoClosableLock ignored = new AutoClosableLock(lock)) {
            actors.entrySet().stream().map(Map.Entry::getValue).forEach(a -> a.shutdown(now));
        }
    }


    @Override
    public boolean unregisterActor(String name) {
        try (AutoClosableLock ignored = new AutoClosableLock(lock)) {
            Actor remove = actors.remove(name);
            if (remove != null) {
                // this actor will not accept any messages anymore. The Accesses should try to get a new instance or fail.
                remove.unregisterGracefully();
                return true;
            }
            return false;
        }
    }

    Optional<Actor> getOptionalActor(String name) {
        return Optional.ofNullable(actors.get(name));
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("ActorSystem{");

        actors.forEach((a, f) -> stringBuilder.append(f.toString()).append("\n"));
        stringBuilder.append(" exec:").append(processorFactory.toString());
        stringBuilder.append("}");

        return stringBuilder.toString();
    }
}
