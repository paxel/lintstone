# lintstone
There is no Thread Thlintstone. Lintstone is a simple actor framework. nough said.


```xml
<dependency>
    <groupId>io.github.paxel</groupId>
    <artifactId>lintstone</artifactId>
    <version><!-- version see release page -->></version>
</dependency>
```
# How to use it?

## Actor

An actor only lives (is executed) if a message is sent to it.
The message defines what the actor does.
The message is given inside a context object for easy type handling.
the context object is also used to respond or send messages to other actors.
the context object is only valid for the one message object that it handles.

## Usage

First you need to init the actorsystem

```java
LintStoneSystem system = LintStoneSystemFactory.create(Executors.newCachedThreadPool());
```

the system creates the actors

```java
LintStoneActorAccess fileCollector = system.registerActor("fileCollector", () -> new FileCollector(cfg), Optional.empty(), ActorSettings.DEFAULT);
...
fileCollector.send(FileCollector.fileMessage(root, readOnly));
```
() -> new FileCollector(cfg) is a factory for creating a FileCollector Actor.
A FileMessage is created and sent to the actor.

The message is enqueued and eventually processed by the actor instance

```java
    @Override
    public void newMessageEvent(LintStoneMessageEventContext mec) {
        mec
                // scan dir = most often
                .inCase(DirMessage.class, this::scanDir)
                // a few files might be added
                .inCase(FileMessage.class, (f, m) -> this.addFile(f.path, f.readOnly, m))
                // all import received
                .inCase(EndMessage.class, this::end)
                // that shouldn't happen at all
                .otherwise((a, b) -> System.out.println("" + a));
    }

    private void addFile(Path f, boolean readOnly, LintStoneMessageEventContext m) {
        long length = f.toFile().length();
        if (length == 0) {
            this.zero++;
        } else {
            fileData += length;
            final LintStoneActorAccess actor = actors.computeIfAbsent(length, k -> {
                return m.registerActor("counter-" + length, () -> new FileComparator(length), Optional.empty(), ActorSettings.DEFAULT);
            });
            actor.send(fileMessage(f, readOnly));
        }
    }
```

The actor delegates the content of the FileMessage to the addFile method.
This method gets the size of the file and creates a FileComparator actor for this size (if not already exists).
then it sends a new fileMessage to that actor.

It is also possible to ask an actor for a response.
It is implemented in a way, that a consumer for the response is sent with the message.
If the asked actor responds to the message, the consumer is called with the response:

* in the thread of the asking actor
* in the thread of the asked actor in case the ask was called from outside the actor system.
This is a good way to get the result from the system in case of a multithreaded process.

```java

for (String text : data) {
    dist.send(text);
}

//finally ask for result

String v = dist.<String>ask(new EndMessage())
               .get(1, TimeUnit.MINUTES);
```


# Benchmarks

```
Benchmark                                  Mode  Cnt       Score      Error  Units
JmhTest.run001ActorOn001Thread            thrpt    5   14807.641 ±  118.880  ops/s
JmhTest.run002ActorOn001Thread            thrpt    5   14793.968 ±  132.940  ops/s
JmhTest.run010ActorOn001Thread            thrpt    5   14767.468 ±  141.431  ops/s
JmhTest.run010ActorOn010Thread            thrpt    5  122679.731 ± 2291.937  ops/s
JmhTest.run020ActorOn020Thread            thrpt    5  165262.649 ± 9270.499  ops/s
JmhTest.run030ActorOn020Thread            thrpt    5  164761.670 ± 5103.291  ops/s
JmhTest.run999ActorOn010Threads           thrpt    5  110869.939 ± 1938.133  ops/s
JmhTest.run999ActorOnWorkStealingThreads  thrpt    5    4200.800 ±  109.647  ops/s
```