# lintstone
There is no Thread Thlintstone. Lintstone is a simple actor framework. nough said.

# How to use it?

## The idea

The idea of this actor system is to be as usable and simple as possible and just replace your normal threading operations.
Normally actor systems instist on immutable messages and short processing time of actors and so on.
LintStone says: you do what you want.
LintStone also says: there might be consequences.

So an Actor is just implementing the Actor interface

```java
package paxel.lintstone.api;

@FunctionalInterface
public interface LintStoneActor {
    void newMessageEvent(LintStoneMessageEventContext mec);
}
```

So implementing an actor can be as easy as this:
```java
        LintStoneActor myActor = mec -> mec.otherwise((o,m)->System.out.print(o));
```

The actor must be registered in the actor System, that will start and stop the actor if there are message for it to process.

```java
        LintStoneSystem system = LintStoneSystemFactory.createLimitedThreadCount(5);
        LintStoneActorAccess syncedOut = system.registerActor(
          "out", // name of the actor. If you need to request it from the system somewhere else
          () ->  mec -> mec.otherwise((o,m)->System.out.print(o)), // the factory to create the actor
          Optional.empty(), // the optional initial message processed by the actor
          ActorSettings.create().build() // the actor settings
        );
```

So now you have a LintStoneActorAccess in your hand. now what?
This interface looks like this:


```java
package paxel.lintstone.api;
public interface LintStoneActorAccess {

    // important
    void send(Object message) throws UnregisteredRecipientException;
    void ask(Object message, ReplyHandler replyHandler) throws UnregisteredRecipientException;

    // just sugar
    boolean exists();
    int getQueuedMessagesAndReplies();
    long getProcessedMessages();
    long getProcessedReplies();
    String getName();
}
```

So you can send objects to the actor and ask the actor about an object.

### send(Object)

Our actor implementation is called for each object that we send sequentially, and the `mec.otherwise(Object,mec)` method is a fallback that in this case will always be called and print the object to standard out. So what is the gain here? We can send messages multithreaded to our actor and they will be processed one after another, never corrupting the data on standard out. Cool right? To do this in a thread we would have to add a queue where we put the messages in and implement a thread that pulls them out.

But this is the simplest of all examples. There is so much more POWER!




