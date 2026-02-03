package paxel.lintstone.api;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;

public class MessageAccessTest {

    @Test
    void testOnlyFirstMatch() {
        LintStoneSystem system = LintStoneSystemFactory.create();
        AtomicInteger matchCount = new AtomicInteger(0);

        LintStoneActor actor = mec -> {
            mec.inCase(String.class, (s, ctx) -> matchCount.incrementAndGet())
               .inCase(Object.class, (o, ctx) -> matchCount.incrementAndGet())
               .otherwise((o, ctx) -> matchCount.incrementAndGet());
        };

        LintStoneMessageEventContext mockCtx = createMockContext();
        MessageAccess access = new MessageAccess();
        access.reset("test", mockCtx);

        access.inCase(String.class, (s, ctx) -> matchCount.incrementAndGet())
              .inCase(Object.class, (o, ctx) -> matchCount.incrementAndGet())
              .otherwise((o, ctx) -> matchCount.incrementAndGet());

        assertThat("Only the first matching inCase should be executed", matchCount.get(), is(1));
    }

    @Test
    void testOtherwiseOnlyIfNoMatch() {
        LintStoneSystem system = LintStoneSystemFactory.create();
        AtomicBoolean otherwiseExecuted = new AtomicBoolean(false);

        MessageAccess access = new MessageAccess();
        access.reset("test", createMockContext());

        access.inCase(Integer.class, (i, ctx) -> {})
              .otherwise((o, ctx) -> otherwiseExecuted.set(true));

        assertThat("otherwise should be executed if no inCase matched", otherwiseExecuted.get(), is(true));

        otherwiseExecuted.set(false);
        access.reset("test", createMockContext());
        access.inCase(String.class, (s, ctx) -> {})
              .otherwise((o, ctx) -> otherwiseExecuted.set(true));

        assertThat("otherwise should NOT be executed if an inCase matched", otherwiseExecuted.get(), is(false));
    }

    @Test
    void testInheritanceMatch() {
        AtomicBoolean matched = new AtomicBoolean(false);
        MessageAccess access = new MessageAccess();
        access.reset(new StringBuilder("test"), createMockContext());

        access.inCase(CharSequence.class, (cs, ctx) -> matched.set(true));

        assertThat("Should match superclass/interface", matched.get(), is(true));
    }

    private LintStoneMessageEventContext createMockContext() {
        return new LintStoneMessageEventContext() {
            @Override public <T> MessageAccess inCase(Class<T> clazz, LintStoneEventHandler<T> consumer) { return null; }
            @Override public void otherwise(LintStoneEventHandler<Object> message) {}
            @Override public void reply(Object msg) {}
            @Override public void tell(String name, Object msg) {}
            @Override public void tell(String name, Object msg, java.time.Duration delay) {}
            @Override public void ask(String name, Object msg, ReplyHandler handler) {}
            @Override public <F> java.util.concurrent.CompletableFuture<F> ask(String name, Object msg) { return null; }
            @Override public LintStoneActorAccessor getActor(String name) { return null; }
            @Override public LintStoneActorAccessor registerActor(String name, LintStoneActorFactory factory, Object initMessage, ActorSettings settings) { return null; }
            @Override public LintStoneActorAccessor registerActor(String name, LintStoneActorFactory factory, ActorSettings settings) { return null; }
            @Override public boolean unregister() { return false; }
            @Override public String getName() { return null; }
            @Override public boolean unregister(String actorName) { return false; }
        };
    }
}
