package paxel.lintstone.impl;

import java.util.Optional;

import paxel.lintstone.api.LintStoneActorAccess;
import paxel.lintstone.api.LintStoneActorFactory;
import paxel.lintstone.api.LintStoneEventHandler;
import paxel.lintstone.api.LintStoneMessageEventContext;
import paxel.lintstone.api.NoSenderException;
import paxel.lintstone.api.UnregisteredRecipientException;

public class MessageContext implements LintStoneMessageEventContext {

    private final ActorSystem actorSystem;
    private final SelfUpdatingActorAccess self;
    private Optional<SelfUpdatingActorAccess> sender;
    private Object message;
    private boolean match;

    public MessageContext(ActorSystem actorSystem, SelfUpdatingActorAccess self) {
        this.actorSystem = actorSystem;
        this.self = self;
    }

    @Override
    public <T> LintStoneMessageEventContext inCase(Class<T> clazz, LintStoneEventHandler<T> consumer) {
        if (!match && clazz.isAssignableFrom(message.getClass())) {
            match = true;
            consumer.handle(clazz.cast(message), this);
        }
        return this;
    }

    @Override
    public void otherwise(LintStoneEventHandler<Object> catchAll) {
        if (!match) {
            catchAll.handle(message, this);
        }
    }

    @Override
    public void reply(Object msg) throws NoSenderException, UnregisteredRecipientException {
        sender.orElseThrow(() -> new NoSenderException("Message has no Sender"))
                .send(msg, self);
    }

    @Override
    public void send(String name, Object msg) throws UnregisteredRecipientException {
        Optional<Actor> actor = actorSystem.getActor(name);
        if (!actor.isPresent()) {
            throw new UnregisteredRecipientException("Actor with name " + name + " does not exist");
        }
        actor.get().send(msg, Optional.of(self));
    }

    @Override
    public LintStoneActorAccess getActor(String name) {
        // give a empty ref, that is filled on demand.
        return new SelfUpdatingActorAccess(name, null, actorSystem, Optional.of(self));
    }

    @Override
    public LintStoneActorAccess registerActor(String name, LintStoneActorFactory factory, Optional<Object> initMessage) {
        return actorSystem.registerActor(name, factory, initMessage, Optional.of(self));
    }

    @Override
    public LintStoneActorAccess registerSingleSourceActor(String name, LintStoneActorFactory factory, Optional<Object> initMessage) {
        return actorSystem.registerMultiSourceActor(name, factory, initMessage, Optional.of(self));
    }

    @Override
    public LintStoneActorAccess registerMultiSourceActor(String name, LintStoneActorFactory factory, Optional<Object> initMessage) {
        return actorSystem.registerMultiSourceActor(name, factory, initMessage, Optional.of(self));
    }

    void init(Object message, Optional<SelfUpdatingActorAccess> sender) {
        match = false;
        this.message = message;
        this.sender = sender;
    }

    @Override
    public boolean unregister() {
        return actorSystem.unregisterActor(self.getName());
    }

    @Override
    public String getName() {
        return self.getName();
    }

    @Override
    public boolean unregister(String actorName) {
        return actorSystem.unregisterActor(actorName);
    }
}
