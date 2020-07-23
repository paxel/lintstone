package paxel.lintstone.impl;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import paxel.bulkexecutor.GroupingExecutor;
import paxel.bulkexecutor.SequentialProcessor;
import paxel.lintstone.api.LintStoneActor;
import paxel.lintstone.api.LintStoneActorAccess;
import paxel.lintstone.api.LintStoneActorFactory;
import paxel.lintstone.api.LintStoneSystem;

public class ActorSystem implements LintStoneSystem {

    private final Map<String, Actor> actors = Collections.synchronizedMap(new HashMap<>());
    private final GroupingExecutor groupingExecutor;
    private final ExecutorService executorService;

    public ActorSystem(ExecutorService executorService) {
        this.executorService = executorService;
        groupingExecutor = new GroupingExecutor(executorService);
    }

    @Override
    public LintStoneActorAccess registerMultiSourceActor(String name, LintStoneActorFactory factory, Optional<Object> initMessage) {
        Optional<SelfUpdatingActorAccess> sender = Optional.empty();
        return registerActor(name, factory, initMessage, sender, groupingExecutor.createMultiSourceSequentialProcessor());
    }

    @Override
    public LintStoneActorAccess registerActor(String name, LintStoneActorFactory factory, Optional<Object> initMessage) {
        return registerMultiSourceActor(name, factory, initMessage);
    }

    @Override
    public LintStoneActorAccess registerSingleSourceActor(String name, LintStoneActorFactory factory, Optional<Object> initMessage) {
        Optional<SelfUpdatingActorAccess> sender = Optional.empty();
        return registerActor(name, factory, initMessage, sender, groupingExecutor.createSingleSourceSequentialProcessor());
    }


    LintStoneActorAccess registerActor(String name, LintStoneActorFactory factory, Optional<Object> initMessage, Optional<SelfUpdatingActorAccess> sender) {
        return registerActor(name, factory, initMessage, sender, groupingExecutor.createMultiSourceSequentialProcessor());
    }

    LintStoneActorAccess registerSingleSourceActor(String name, LintStoneActorFactory factory, Optional<Object> initMessage, Optional<SelfUpdatingActorAccess> sender) {
        return registerActor(name, factory, initMessage, sender, groupingExecutor.createSingleSourceSequentialProcessor());
    }

    LintStoneActorAccess registerMultiSourceActor(String name, LintStoneActorFactory factory, Optional<Object> initMessage, Optional<SelfUpdatingActorAccess> sender) {
        return registerActor(name, factory, initMessage, sender, groupingExecutor.createMultiSourceSequentialProcessor());
    }

    private LintStoneActorAccess registerActor(String name, LintStoneActorFactory factory, Optional<Object> initMessage, Optional<SelfUpdatingActorAccess> sender, SequentialProcessor sequentialProcessor) {
        synchronized (actors) {
            Actor existing = actors.get(name);
            if (existing != null) {
                return new SelfUpdatingActorAccess(name, existing, this, sender);
            }
            LintStoneActor actorInstance = factory.create();
            Actor newActor = new Actor(name, actorInstance, sequentialProcessor);
            newActor.setMec(new MessageContext(this, new SelfUpdatingActorAccess(name, newActor, this, sender)));
            // actor receives the initMessage as first message.
            initMessage.ifPresent(msg -> newActor.send(msg, Optional.empty()));
            actors.put(name, newActor);
            return new SelfUpdatingActorAccess(name, newActor, this, sender);
        }
    }

    @Override
    public void shutDown() {
        executorService.shutdown();
    }

    @Override
    public void shutDownAndWait() throws InterruptedException {
        executorService.shutdown();
        //wait forever and a day
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.HOURS);
    }

    @Override
    public boolean shutDownAndWait(Duration timeout) throws InterruptedException {
        executorService.shutdown();
        return executorService.awaitTermination(timeout.getSeconds(), TimeUnit.SECONDS);
    }

    @Override
    public void shutDownNow() {
        // who wants to shutdown now doesnÄt want to know about the processes.
        executorService.shutdownNow();
    }

    @Override
    public boolean unregisterActor(String name) {
        synchronized (actors) {
            Actor remove = actors.remove(name);
            if (remove != null) {
                // this actor will not accept any messages anymore. The Accessess should try to get a new instance or fail.
                remove.unregister();
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
}
