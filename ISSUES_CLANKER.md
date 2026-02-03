# LintStone: Potential Bottlenecks, Bugs, and Security Considerations

This document summarizes findings from a focused review of the core actor system implementation. It highlights potential performance bottlenecks, correctness bugs, concurrency risks, and security considerations, and proposes remediation ideas.

## Summary of Key Findings

- ~~Critical: SimpleScheduler may hang on shutdown when idle (no jobs queued).~~
- ~~Critical: SimpleScheduler can drop jobs scheduled for the exact same instant due to `compareTo` equality.~~
- ~~DoS risk: Unbounded message queues for `tell(...)` (no backpressure) can exhaust memory.~~
- ~~Concurrency risk: `SelfUpdatingActorAccessor.actor` is not `volatile` and may be used across threads.~~
- ~~Backpressure edge cases on shutdown/abort may leave producers blocked.~~
- ~~Timing/accuracy: Scheduler has coarse minimum wake-up (≥100 ms) and uses multiple `Instant.now()` calls.~~
- ~~Minor: Scheduling still accepted after shutdown; jobs will never run.~~
- ~~Minor: Dead code and visibility modifiers can be tightened.~~

---

## Detailed Findings

~~### 1) SimpleScheduler shutdown can hang when idle (no jobs queued)~~
FIXED

~~### 2) SimpleScheduler may drop tasks scheduled for the exact same time~~
FIXED

~~### 3) Scheduling allowed after shutdown~~
FIXED

~~### 4) Coarse timing and extra `now()` calls in scheduler~~
FIXED

~~### 5) Potential producer starvation on shutdown/backpressure~~
FIXED

~~### 6) Unbounded message queue for `tell(...)` can cause memory exhaustion~~
FIXED

~~### 7) Concurrency visibility risk in SelfUpdatingActorAccessor~~
FIXED

~~### 8) Interrupted status is swallowed~~
FIXED

~~### 9) Minor cleanups and code hygiene~~
FIXED

### 10) Security considerations

- The library is in-process; there is no network, file I/O, or deserialization by default. However:
  - DoS risk (Finding #6) is a security concern when exposed in multi-tenant or untrusted environments.
  - Exception logging includes message contents (`toString()` of messages) which may leak sensitive data if user payloads contain PII. Consider configurable redaction or structured logging.

---

## Recommended Next Steps

1. ~~Fix `SimpleScheduler` issues:~~
   - ~~Signal condition in `shutDown()` and guard `runLater` after shutdown.~~
   - ~~Add a tiebreaker to `ScheduledRunnable.compareTo`.~~
   - ~~Reduce minimum wait and compute `now` once per loop.~~
2. ~~Improve backpressure and shutdown semantics in `SequentialProcessorImpl`:~~
   - ~~Replace magic permit release; consider a bounded queue and clearer shutdown signaling to producers.~~
   - ~~Restore interrupt flag on `InterruptedException`.~~
3. ~~Provide configurable queue limits and default backpressure behavior for `tell(...)` via `ActorSettings`.~~
4. ~~Make `SelfUpdatingActorAccessor.actor` `volatile` or document/enforce single-threaded use.~~
5. ~~Add tests:~~
   - ~~Scheduler: identical-instant tasks; shutdown while idle; post-shutdown `runLater` behavior.~~
   - ~~Backpressure: producers blocked across shutdown sequences.~~
6. Review logging for sensitive payloads and add redaction options.

---

## Non-Goals/Notes
- The current review focused on core concurrency paths. Public API stability and behavior changes (e.g., making `getActor` throw on missing actors) were not evaluated for compatibility.
- The system already leverages virtual threads (`newVirtualThreadPerTaskExecutor`), which is generally good for scalability; recommended changes aim to preserve that model while improving resilience.
