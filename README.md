# lintstone
There is no Thread Thlintstone. Lintstone is a simple actor framework. nough said.

[![justforfunnoreally.dev badge](https://img.shields.io/badge/justforfunnoreally-dev-9ff)](https://justforfunnoreally.dev)

```xml
        <dependency>
            <groupId>io.github.paxel</groupId>
            <artifactId>lintstone-actor-system</artifactId>
            <version>2.0.1</version>
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

First you need to init the actorsystem.
Since JAVA 21 the ActorSystem uses Virtual Threads internally.
Each Actor runs on one virtual Thread and only gets active if a message is available.

```java
LintStoneSystem system = LintStoneSystemFactory.create();
```

the system creates the actors

```java
LintStoneActorAccess fileCollector = system.registerActor("fileCollector", () -> new FileCollector(cfg), ActorSettings.DEFAULT);
...
fileCollector.tell(FileCollector.fileMessage(root, readOnly));
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
                return m.registerActor("counter-" + length, () -> new FileComparator(length), ActorSettings.DEFAULT);
            });
            actor.tell(fileMessage(f, readOnly));
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
    dist.tell(text);
}

//finally ask for result

String v = dist.<String>ask(new EndMessage())
               .get(1, TimeUnit.MINUTES);
```
## Usage with backpressure

In some situations you create too many events and want to prevent to flood the system.
Therefore the tellWithBackPressure method was introduced.
It blocks the tell until the unprocessed message number is less than the given value.

```java

for (String text : data) {
    dist.tellWithBackPressure(text,1_000_000);
}

```

There is no ask with BackPressure (yet).
Ask should be used at the end of batch and not in mass, because it is less effective

Be aware that if you send messages with backpressure in a circle you might cause deadlocks!

# Benchmarks

Benchmark: 
* create system with x actors. 
* send 1000 messages to each actor
* finish and remove each actor
* wait until the result was sent to the final actor
* shutdown the system
```
Benchmark                                  Mode  Cnt        Score         Error  Units
ActorBenchmark.askSingleActor             thrpt    3    44921.216 ±    37077.887  ops/s
ActorBenchmark.tellSingleActor            thrpt    3  1843570.182 ± 10501679.242  ops/s
ActorBenchmark.tellSingleActorContention  thrpt    3  3022977.051 ±   218224.576  ops/s
ActorBenchmark.tellTwoActors              thrpt    3  1583756.916 ±   477648.471  ops/s
JmhTest.run_50000_Actors                  thrpt    3  2140462.739 ±  3631173.557  ops/s
```

A better test would be:
* setup the system before the benchmark
* send the 1000 messages to each actor
* ask each actor for the sum
  * remove each actor
* shutdown the system after the benchmark 

