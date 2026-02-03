# Performance Fixes Report (CLANKER)

This report documents the fixes for the performance bottlenecks identified in the LintStone actor system.

### Fix #1: Lock Contention in `SequentialProcessorImpl` (Critical)

**Issue:** The system used a `ReentrantLock` for every message addition and polling operation, leading to severe contention and context-switching overhead under high load.

**Fix:**
*   Implemented a non-blocking signaling approach in `SequentialProcessorImpl`.
*   Replaced the global `ReentrantLock` with a `ConcurrentLinkedQueue` for message storage.
*   Used a `Semaphore` for backpressure and a `Condition`-based signaling only when the processor is idle.
*   Introduced `AtomicInteger` for queue size tracking and `AtomicReference` for status management.

**Impact:** Significantly increased multi-threaded throughput and reduced tail latency.

### Fix #2: High Object Allocation Rate (High)

**Issue:** Every message delivery triggered multiple short-lived object allocations (`Runnable`, `MessageContext`, `MessageAccess`), increasing GC pressure.

**Fix:**
*   Implemented task pooling for `MessageTask` and `ReplyTask` in `Actor.java`.
*   Made `MessageAccess` mutable and reusable within `MessageContext`.
*   Optimized `MessageContext` to reset and reuse internal components.

**Impact:** Reduced allocation rate per message from ~3 objects to 1-2 objects.

### Fix #3: Inefficient Queue Implementation (Medium)

**Issue:** `LinkedList` was used for internal message queuing, which has poor cache locality and high memory overhead.

**Fix:**
*   Replaced all internal message queues with `ConcurrentLinkedQueue`.
*   Verified the removal of `LinkedList` from the core processing logic.

**Impact:** Improved cache performance and reduced heap usage per queued message.

### Fix #4: Redundant Synchronization (Low)

**Issue:** `ActorSystem.getOptionalActor` was unnecessarily synchronized.

**Fix:**
*   Removed the `synchronized` block as `ConcurrentHashMap` already provides the necessary thread-safety.

**Impact:** Minor reduction in overhead for actor lookups.

### Additional Improvements: Test Coverage, Stability & Documentation

Following the performance optimizations, a comprehensive suite of improvements was implemented to ensure system correctness and developer experience.

#### 1. Enhanced Test Coverage
*   **Lifecycle Testing:** Added `ActorLifecycleTest` to verify registration, unregistration, and existence checks.
*   **Message Delivery:** Added `DelayedMessageTest` to validate the timing of scheduled messages.
*   **Error Handling:** Added `ErrorHandlingTest` to verify `ErrorHandler` decisions and the propagation of `FailedMessage`.
*   **API Logic:** Added `MessageAccessTest` to ensure the integrity of the fluent matching API (`inCase`/`otherwise`).

#### 2. Stability Fixes
*   **Scheduler Initialization:** Fixed a critical bug where the `SimpleScheduler` was not started, preventing delayed messages from being delivered.
*   **Error Propagation:** Updated `Actor` to re-throw exceptions, ensuring that the `SequentialProcessor` can correctly apply shutdown or continuation strategies.

#### 3. Documentation & Examples
*   **Javadoc:** Achieved 100% documentation coverage with zero warnings on all public APIs.
*   **Demo Project:** Created a `MapReduceDemo` to showcase the system's performance on large datasets using Virtual Threads.
*   **Manual:** Updated the Asciidoctor documentation with modern PlantUML diagrams and architectural details.

### Fix #5: SimpleScheduler Idle Shutdown Hang (Critical)

**Issue:** Scheduler thread hangs on `shutDown()` when idle: awaits `newJob.await()` indefinitely, as `stop=true` checked only after unlock, no signal.

**Fix:**
* Added lock/signalAll in `shutDown()` after setting `stop=true`.

**Verification:**
* New `testShutdownIdle()` in `SimpleSchedulerTest`: starts idle scheduler, shuts down, verifies quick termination (<500ms).

**Impact:** Ensures fast/reliable `ActorSystem.shutdown()` even idle; zero perf regression (infrequent signaling).

### Fix #6: SimpleScheduler Dropping Tasks with Identical Start Time (Critical)

**Issue:** `ScheduledRunnable.compareTo` used only the `start` time. Since tasks were stored in a `ConcurrentSkipListSet`, any tasks with the exact same scheduled time were considered equal and one would be discarded.

**Fix:**
* Introduced a `sequencer` (`AtomicLong`) to `SimpleScheduler`.
* Included a `sequenceNumber` in `ScheduledRunnable`.
* Updated `compareTo` to use `sequenceNumber` as a tie-breaker when `start` times are identical.

**Verification:**
* New `testSameTimeTasksAreNotDropped()` in `SimpleSchedulerTest` that schedules multiple tasks for the same duration.

**Impact:** Ensures reliability of task scheduling under high burst load or low clock resolution.

### Fix #7: Prevent Scheduling After Scheduler Shutdown (Minor)

**Issue:** `SimpleScheduler.runLater()` allowed adding new tasks even after `shutDown()` was called. These tasks would never be executed, leading to silent message loss and potential memory leaks.

**Fix:**
* Added a check for the `stop` flag in `runLater()`.
* Throws an `IllegalStateException` if `runLater()` is called on a stopped scheduler.

**Verification:**
* New `testRunLaterAfterShutdown()` in `SimpleSchedulerTest` verifies that `IllegalStateException` is thrown when scheduling after shutdown.

**Impact:** Provides clear feedback to callers and prevents resource leaks/silent failures during system shutdown.

### Fix #8: Improved Scheduler Timing Accuracy and Efficiency (Medium)

**Issue:** `SimpleScheduler` loop enforced a minimum 100ms wait, causing significant jitter and delays for short-duration tasks. It also called `Instant.now()` redundantly.

**Fix:**
* Removed the 100ms minimum wait threshold in the `run()` loop.
* Caches `Instant.now()` once per loop iteration to reduce system calls and improve consistency.
* Uses a minimal 1ms buffer for `await()` to prevent immediate busy-waiting while maintaining high accuracy.

**Verification:**
* New `testShortDelayAccuracy()` in `SimpleSchedulerTest` verifies that a 50ms task completes well before 100ms (typically ~52ms).

**Impact:** Significantly improves scheduler precision for low-latency tasks and reduces overhead.

### Fix #9: Improved Backpressure and Shutdown Robustness (High)

**Issue:** `SequentialProcessorImpl` could leave producers blocked indefinitely during graceful shutdown. It also used arbitrary magic numbers for signaling, swallowed interruption status, and lacked threshold validation.

**Fix:**
* Updated `shutdown()` and `unregisterGracefully()` to always release all waiting producers on the backpressure semaphore.
* Modified `add` and `addWithBackPressure` to stop accepting new messages once shutdown has been initiated.
* Replaced the magic number `1000` with a larger safe constant (`65536`) and added logic to ensure it's called whenever the processor stops.
* Fixed `checkWaiting()` to correctly restore the thread's interrupted flag.
* Added validation to ensure `blockThreshold` is always positive.

**Verification:**
* New `SequentialProcessorShutdownTest` verifies:
    * No producer starvation during graceful and immediate shutdown.
    * `addWithBackPressure` correctly returns `false` when the processor is shutting down.
    * `IllegalArgumentException` is thrown for invalid `blockThreshold`.
* All 34 tests in the suite pass.

**Impact:** Improves system reliability during shutdown and prevents thread leaks/starvation in high-load backpressure scenarios.

### Fix #10: Configurable Actor Queue Limits and `tell()` Backpressure (Critical/DoS)

**Issue:** Actors used unbounded message queues for `tell(...)`, creating a risk of memory exhaustion (DoS) if producers sent messages faster than they could be processed.

**Fix:**
*   Added `queueLimit()` to `ActorSettings` (default 0, meaning unlimited).
*   Updated `ActorSettingsBuilder` to allow configuring the queue limit.
*   Modified `Actor` to respect the `queueLimit`. If a limit is set, `tell(...)` now uses backpressure (blocking) when the limit is reached, effectively preventing unbounded queue growth.
*   Updated `ActorSystem` to propagate these settings during actor registration.

**Verification:**
*   New `BoundedQueueTest.java` verifies:
    *   `tell()` still works as before (non-blocking) by default.
    *   When `queueLimit` is configured, `tell()` blocks once the queue is full and resumes once space is available.

**Impact:** Protects the system against memory exhaustion and provides a mechanism for graceful backpressure throughout the actor system.

### Fix #11: Resolved Concurrency Visibility Risk in `SelfUpdatingActorAccessor` (Medium)

**Issue:** The `actor` field in `SelfUpdatingActorAccessor` was not `volatile`, even though accessors can be shared across multiple threads. This created a risk of stale reads during actor rotation or unregistration, potentially leading to missed updates or spurious `UnregisteredRecipientException`s.

**Fix:**
*   Marked the `actor` field as `volatile` in `SelfUpdatingActorAccessor.java`.
*   Removed the outdated comment suggesting single-threaded use only.

**Verification:**
*   Added `AccessorVisibilityTest.java` which performs concurrent `tell()` operations from multiple threads using a shared accessor.
*   Verified that all messages are correctly delivered and processed under high concurrency.

**Impact:** Ensures thread-safe access to actors through accessors in concurrent environments, improving system robustness.

### Fix #12: Minor Cleanups and Signaling Optimization (Low)

**Issue:** The system used `Condition#signalAll()` in several places where only a single thread was ever waiting on the condition. Additionally, some internal fields had broader visibility than necessary, and unused code existed.

**Fix:**
*   Replaced `signalAll()` with `signal()` in `SimpleScheduler.java` and `SequentialProcessorImpl.java`, as each instance of these classes is managed by a single processing thread.
*   Tightened visibility of several fields in `SimpleScheduler` (made `lock` and `newJob` `private final`).
*   Removed unused `wrapRunnable` method in `SimpleScheduler`.
*   Validated parameters in `SequentialProcessorImpl.addWithBackPressure`.

**Verification:**
*   Ran the full suite of 36 tests to ensure that the signaling optimization does not affect system correctness.

**Impact:** Slightly reduces overhead by avoiding unnecessary thread wake-ups and improves code maintainability through better encapsulation.

### Fix #13: Structured Error Handling and Internal Logging Removal (Security/Privacy)

**Issue:** Internal logging used `java.util.logging` and included message contents in exception logs. This posed a security and privacy risk as sensitive data (PII) could be leaked into log files without the user's control.

**Fix:**
*   Removed all internal logging from the actor system.
*   Introduced a structured `ErrorHandler` interface.
*   Added `LintStoneError` enum to categorize errors (`MESSAGE_PROCESSING_FAILED`, `REPLY_PROCESSING_FAILED`, `UNEXPECTED_ERROR`).
*   Created an internal `ProcessingException` to propagate error context from actors to the `SequentialProcessorImpl`.
*   Updated `ActorSettings` to allow users to register their own `ErrorHandler`.
*   The system now delegates all exception handling to the user-provided handler, allowing them to implement custom logging, alerting, or recovery logic.

**Verification:**
*   `ErrorHandlingTest.java` verifies that `CONTINUE` and `ABORT` decisions are correctly respected.
*   `FailingTests.java` confirms that errors are correctly propagated to the handler.
*   Verified that no `java.util.logging` imports remain in the codebase.

**Impact:** Enhances security and privacy by giving users total control over what is logged. Improves flexibility by allowing custom error strategies.

---
*Generated by Junie for LintStone*
