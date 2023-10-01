package paxel.lintstone.impl;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import paxel.lintstone.api.*;

public class ActorSystem implements LintStoneSystem {

    private final Map<String, Actor> actors = Collections.synchronizedMap(new HashMap<>());
    private final GroupingExecutor groupingExecutor;

    public ActorSystem() {
        groupingExecutor = new GroupingExecutor();
    }

    @Override
    public LintStoneActorAccessor registerActor(String name, LintStoneActorFactory factory, ActorSettings settings, Object initMessage) {
        return registerActor(name, factory, null, settings, initMessage);
    }

    @Override
    public LintStoneActorAccessor registerActor(String name, LintStoneActorFactory factory, ActorSettings settings) {
        return registerActor(name, factory, null, settings, null);
    }

    LintStoneActorAccessor registerActor(String name, LintStoneActorFactory factory, SelfUpdatingActorAccessor sender, ActorSettings settings, Object initMessage) {
        SequentialProcessorBuilder sequentialProcessorBuilder = groupingExecutor.create();
        sequentialProcessorBuilder.setErrorHandler(settings.errorHandler());
        return registerActor(name, factory, initMessage, sender, sequentialProcessorBuilder);
    }


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


    @Override
    public void shutDown() {
        shutdownActors(false);
        groupingExecutor.shutdown();
    }

    @Override
    public void shutDownAndWait() throws InterruptedException {
        shutdownActors(false);
        groupingExecutor.shutdown();
        //wait forever and a day
        groupingExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.HOURS);
    }

    @Override
    public boolean shutDownAndWait(Duration timeout) throws InterruptedException {
        shutdownActors(false);
        groupingExecutor.shutdown();
        return groupingExecutor.awaitTermination(timeout.getSeconds(), TimeUnit.SECONDS);
    }

    @Override
    public void shutDownNow() {
        shutdownActors(true);
        groupingExecutor.shutdownNow();
    }

    private void shutdownActors(boolean now) {
        synchronized (actors) {
            actors.entrySet().stream().map(Map.Entry::getValue).forEach(a -> a.shutdown(now));
        }
    }


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

    Optional<Actor> getActor(String name) {
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
