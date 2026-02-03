# LintStone: Potential Bottlenecks, Bugs, and Security Considerations

This document summarizes findings from a focused review of the core actor system implementation. It highlights potential performance bottlenecks, correctness bugs, concurrency risks, and security considerations, and proposes remediation ideas.

## Summary of Key Findings

- Critical: SimpleScheduler may hang on shutdown when idle (no jobs queued).
- Critical: SimpleScheduler can drop jobs scheduled for the exact same instant due to `compareTo` equality.
- DoS risk: Unbounded message queues for `tell(...)` (no backpressure) can exhaust memory.
- Concurrency risk: `SelfUpdatingActorAccessor.actor` is not `volatile` and may be used across threads.
- Backpressure edge cases on shutdown/abort may leave producers blocked.
- Timing/accuracy: Scheduler has coarse minimum wake-up (≥100 ms) and uses multiple `Instant.now()` calls.
- Minor: Scheduling still accepted after shutdown; jobs will never run.
- Minor: Dead code and visibility modifiers can be tightened.

---

## Detailed Findings

~~### 1) SimpleScheduler shutdown can hang when idle (no jobs queued)
File: `src/main/java/paxel/lintstone/impl/SimpleScheduler.java`

- In the run loop, if `jobs` is empty, the thread waits with `newJob.await()` (no timeout). The `shutDown()` method only sets `stop = true` and does not signal the condition. If the scheduler is idle when shutdown is called, the scheduler thread can block indefinitely.
- Impact: `ActorSystem.shutDown()`/`shutDownAndWait()` call `scheduler.shutDown()`, but the scheduler thread may keep running (or be blocked forever), leading to thread leak or delayed termination.
- Suggested fix:
  - In `shutDown()`, acquire the lock and `newJob.signalAll()` after flipping `stop` to wake the waiting thread.
  - Alternatively, use `awaitNanos` with a short timeout when `jobs.isEmpty()` so the loop periodically checks `stop`.~~

~~### 2) SimpleScheduler may drop tasks scheduled for the exact same time
File: `SimpleScheduler.java`

- The `ConcurrentSkipListSet` uses `ScheduledRunnable.compareTo` based only on `start`:
  ```java
  private record ScheduledRunnable(Instant start, Runnable runnable) implements Comparable<ScheduledRunnable> {
      @Override public int compareTo(ScheduledRunnable o) { return this.start.compareTo(o.start); }
  }
  ```
  Two tasks with identical `start` compare as 0 and are considered equal in the set, so one is discarded.
- Impact: Lost scheduled executions (hard-to-reproduce time-based bug under burst scheduling).
- Suggested fix:
  - Include a strictly increasing sequence number or tie-breaker (e.g., `System.identityHashCode(runnable)` or an `AtomicLong`) in `compareTo` to ensure strict ordering without equality collisions.~~

### 3) Scheduling allowed after shutdown
File: `SimpleScheduler.java`

- `runLater(...)` does not check `stop`. After shutdown, new jobs can still be added, but will never be executed.
- Impact: Silent message loss and potential memory growth.
- Suggested fix: Guard `runLater` with `if (stop.get()) throw ...` or ignore and log; or return a boolean indicating acceptance.

### 4) Coarse timing and extra `now()` calls in scheduler
File: `SimpleScheduler.java`

- The loop enforces a minimum wait of 100 ms: `await(Math.max(100L, delta + 10), ...)` which reduces timing accuracy for short delays (e.g., 10–50 ms).
- `Instant.now()` is called multiple times per iteration; compute once to reduce overhead and skew.
- Impact: Larger-than-expected delays and jitter; unnecessary system calls.
- Suggested fix: Cache `now` once per loop iteration; use smaller minimum wait or adaptive strategy.

### 5) Potential producer starvation on shutdown/backpressure
Files: `SequentialProcessorImpl.java`

- `addWithBackPressure(...)` blocks when `queueSize >= threshold` by `backPressureSemaphore.acquire()`. On `shutdown(false)` (graceful) there is no mass release of waiting producers; the release only occurs when consumers poll items. If a processor stops progressing for other reasons, producers can remain blocked for prolonged periods.
- On `shutdown(true)` (immediate), there is a release of 1000 permits, which is arbitrary.
- Impact: Potentially blocked producer threads during shutdown/abort sequences.
- Suggested fix:
  - On graceful shutdown initiate a bounded drain with periodic releases, or switch to a bounded queue (e.g., `LinkedBlockingQueue`) where `put`/`offer` semantics are clearer.
  - Replace magic number `1000` with a calculation based on the number of waiting producers or use `drainPermits`/`reducePermits` appropriately.

### 6) Unbounded message queue for `tell(...)` can cause memory exhaustion
Files: `SequentialProcessorImpl.java`, `Actor.java`

- `tell(...)` enqueues without backpressure. While `tellWithBackPressure(...)` exists, external callers can still use `tell(...)`, allowing unbounded growth of `queuedRunnables`.
- Impact: Denial-of-service risk under high input load.
- Suggested fix: Provide configuration to cap queue size per actor; optionally make `tell(...)` respect a default backpressure threshold (configurable in `ActorSettings`).

### 7) Concurrency visibility risk in SelfUpdatingActorAccessor
File: `SelfUpdatingActorAccessor.java`

- Field `actor` is explicitly “not volatile as we expect to be used single-threaded”. In practice, accessors may be shared across threads (e.g., injected into multiple components). Without `volatile`, there can be stale reads/writes during actor rotation/unregister, leading to spurious `UnregisteredRecipientException` or missed updates.
- Impact: Rare, data-race dependent failures in highly concurrent scenarios.
- Suggested remedies:
  - Make `actor` `volatile` or guard with `VarHandle`/`AtomicReference`.
  - Document thread-safety contract of accessors and enforce single-threaded use in API, or create per-thread accessors.

### 8) Interrupted status is swallowed
Files: `SequentialProcessorImpl.java`

- In `checkWaiting()`, when `await` throws `InterruptedException`, the method returns `false` but does not restore the interrupted flag.
- Impact: Loss of interruption information; may hinder upper-layer shutdown logic relying on `Thread.interrupted()`.
- Suggested fix: Call `Thread.currentThread().interrupt()` before returning.

### 9) Minor cleanups and code hygiene

- `SimpleScheduler.wrapRunnable` is unused; can be removed.
- Several fields in `SimpleScheduler` (`lock`, `newJob`) can be `private`.
- Consider using `Condition#signal` instead of `signalAll` where appropriate.
- Add parameter validation for `addWithBackPressure(..., blockThreshold)` to prevent accidental zero/negative thresholds.

### 10) Security considerations

- The library is in-process; there is no network, file I/O, or deserialization by default. However:
  - DoS risk (Finding #6) is a security concern when exposed in multi-tenant or untrusted environments.
  - Exception logging includes message contents (`toString()` of messages) which may leak sensitive data if user payloads contain PII. Consider configurable redaction or structured logging.

---

## Recommended Next Steps

1. Fix `SimpleScheduler` issues:
   - Signal condition in `shutDown()` and guard `runLater` after shutdown.
   - Add a tiebreaker to `ScheduledRunnable.compareTo`.
   - Reduce minimum wait and compute `now` once per loop.
2. Improve backpressure and shutdown semantics in `SequentialProcessorImpl`:
   - Replace magic permit release; consider a bounded queue and clearer shutdown signaling to producers.
   - Restore interrupt flag on `InterruptedException`.
3. Provide configurable queue limits and default backpressure behavior for `tell(...)` via `ActorSettings`.
4. Make `SelfUpdatingActorAccessor.actor` `volatile` or document/enforce single-threaded use.
5. Add tests:
   - Scheduler: identical-instant tasks; shutdown while idle; post-shutdown `runLater` behavior.
   - Backpressure: producers blocked across shutdown sequences.
6. Review logging for sensitive payloads and add redaction options.

---

## Non-Goals/Notes
- The current review focused on core concurrency paths. Public API stability and behavior changes (e.g., making `getActor` throw on missing actors) were not evaluated for compatibility.
- The system already leverages virtual threads (`newVirtualThreadPerTaskExecutor`), which is generally good for scalability; recommended changes aim to preserve that model while improving resilience.
