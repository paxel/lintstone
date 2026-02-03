# LintStone

LintStone is a lightweight, high-performance actor framework for Java 21+, built from the ground up to leverage **Virtual Threads** for maximum scalability and simplicity.

[![justforfunnoreally.dev badge](https://img.shields.io/badge/justforfunnoreally-dev-9ff)](https://justforfunnoreally.dev)

## Installation

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.paxel</groupId>
    <artifactId>lintstone-actor-system</artifactId>
    <version>2.0.1</version>
</dependency>
```

## Core Concepts

*   **ActorSystem:** The container and lifecycle manager for all actors. It handles thread scheduling (using Virtual Threads) and actor registration.
*   **Actor:** A stateful object that processes messages sequentially. Each actor is guaranteed to be executed by only one thread at a time.
*   **Message:** Any Java object. Messages are immutable by convention.
*   **Context (MEC):** The `LintStoneMessageEventContext` provided to actors during message processing. It's used to reply, send messages to other actors, or manage actor lifecycles.

## Quick Start

### 1. Initialize the System
```java
LintStoneSystem system = LintStoneSystemFactory.create();
```

### 2. Define an Actor
Actors implement the `LintStoneActor` interface. During initialization, you define a static decision tree to handle messages. This approach minimizes per-message overhead and GC pressure.

```java
public class HelloActor implements LintStoneActor {
    @Override
    public void newMessageEvent(LintStoneMessageEventContext mec) {
        mec.inCase(String.class, (name, context) -> {
            System.out.println("Hello, " + name + "!");
        }).otherwise((msg, context) -> {
            System.out.println("Received unknown message: " + msg);
        });
    }
}
```

### 3. Register and Send Messages
```java
// Register the actor
LintStoneActorAccessor greeter = system.registerActor("greeter", HelloActor::new, ActorSettings.DEFAULT);

// Send a message (Fire-and-forget)
greeter.tell("World");
```

---

## Usage Guide

### Request-Response (Ask)
You can "ask" an actor for a result, which returns a `CompletableFuture`.

```java
// In your main code:
CompletableFuture<Integer> future = calculator.ask(new AddMessage(5, 10));
Integer result = future.get(1, TimeUnit.SECONDS);

// Inside the Actor:
mec.inCase(AddMessage.class, (add, context) -> {
    context.reply(add.a() + add.b());
});
```

### Creating Actors on the Fly
Actors can create other actors using the context.

```java
mec.inCase(SpawnMessage.class, (msg, context) -> {
    LintStoneActorAccessor child = context.registerActor("child-" + msg.id(), 
        ChildActor::new, ActorSettings.DEFAULT);
    child.tell("Wake up!");
});
```

### Delayed Messages
Messages can be scheduled to be sent after a certain duration.

```java
context.tell("targetActor", "Delayed hello", Duration.ofSeconds(5));
```

### Backpressure
To prevent flooding the system when producing messages faster than they can be processed:

```java
// Blocks if more than 1000 messages are already queued for this actor
actor.tellWithBackPressure(bigData, 1000);
```

### Error Handling
LintStone provides a structured error handling mechanism. By default, the system remains silent to prevent leaking sensitive data in logs. You can provide a custom `ErrorHandler` to decide how to handle exceptions.

```java
ActorSettings settings = ActorSettings.create()
    .setErrorHandler((error, description, cause) -> {
        System.err.println("Error: " + error + " - " + description);
        cause.printStackTrace();
        return ErrorHandlerDecision.CONTINUE; // or ABORT to stop the actor
    })
    .build();

system.registerActor("myActor", MyActor::new, settings);
```

The `ErrorHandler` receives:
*   `LintStoneError`: The category of the error (e.g., `MESSAGE_PROCESSING_FAILED`).
*   `description`: A human-readable context of where the error occurred.
*   `cause`: The actual exception that was caught.

You can return `ErrorHandlerDecision.CONTINUE` to keep the actor running or `ErrorHandlerDecision.ABORT` to shut it down immediately.

### Map-Reduce Demo
LintStone is ideal for data-intensive tasks like Map-Reduce. See the full example in `src/test/java/paxel/lintstone/api/example/mapreduce/MapReduceDemo.java`.

```java
// Main orchestration
int numMappers = Runtime.getRuntime().availableProcessors();
for (int i = 0; i < numMappers; i++) {
    system.registerActor("mapper-" + i, WordCountMapper::new, ActorSettings.DEFAULT);
}
system.registerActor("aggregator", WordCountAggregator::new, ActorSettings.DEFAULT);

// Distribute lines to mappers
try (BufferedReader reader = Files.newBufferedReader(csvFile)) {
    String line;
    while ((line = reader.readLine()) != null) {
        system.getActor("mapper-" + (count++ % numMappers)).tell(line);
    }
}
```

---

## Developer Documentation

### Architecture
LintStone is designed for high throughput and low latency.

1.  **Virtual Thread per Actor:** Each actor is assigned a `SequentialProcessor` which runs on a dedicated Virtual Thread when messages are available.
2.  **Wait-Free Enqueuing:** The core message loop uses a `ConcurrentLinkedQueue` for incoming messages, making `tell()` operations effectively non-blocking.
3.  **Task Pooling:** To minimize Garbage Collection pressure, LintStone pools internal task objects (Runnables). This significantly reduces object allocation in high-traffic scenarios.
4.  **Sequential Guarantee:** While the system uses many threads, individual actors are strictly sequential. You don't need `synchronized` blocks or `volatile` fields for an actor's internal state.

### Performance Optimizations
The system has undergone significant optimizations to handle millions of messages per second:
*   **Static Decision Trees:** Actor message handling is pre-compiled into an optimized decision tree during initialization, eliminating definition overhead during message processing.
*   **Lock Reduction:** Replaced heavy `ReentrantLock` usage with signaling semaphores and atomic variables.
*   **Memory Efficiency:** Replaced `LinkedList` with `ConcurrentLinkedQueue` for better cache locality.

### Benchmarks

The following benchmarks show the throughput on a standard development machine (OpenJDK 21, Virtual Threads enabled).

#### How to run benchmarks
You can run the benchmarks using Maven:
```bash
mvn test-compile
java -cp "target/classes:target/test-classes:$(mvn dependency:build-classpath | grep -v '\[INFO\]')" org.openjdk.jmh.Main ActorBenchmark
```
Or for a full system lifecycle test:
```bash
java -cp "target/classes:target/test-classes:$(mvn dependency:build-classpath | grep -v '\[INFO\]')" org.openjdk.jmh.Main JmhTest
```

| Benchmark | Throughput | Description |
| :--- | :--- | :--- |
| `tellSingleActor` | ~2.7M ops/s | Single producer sending to one actor |
| `tellSingleActorContention` | ~3.4M ops/s | 4 producers sending to one actor |
| `askSingleActor` | ~52K ops/s | Request-response roundtrip |
| `tellRoundRobinManyActors` | ~2.5M ops/s | Sending to 1000 different actors |

*Note: Performance may vary based on CPU and message complexity. For accurate measurements, ensure your system is idle and avoid background activity (like mouse movements during very short benchmarks).*

---
## License
GNU LESSER GENERAL PUBLIC LICENSE Version 3

