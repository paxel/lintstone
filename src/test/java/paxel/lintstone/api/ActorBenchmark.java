package paxel.lintstone.api;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class ActorBenchmark {

    private LintStoneSystem system;
    private LintStoneActorAccessor actor1;
    private LintStoneActorAccessor actor2;

    @Setup
    public void setup() {
        system = LintStoneSystemFactory.create();
        actor1 = system.registerActor("actor1", CountingActor::new, ActorSettings.DEFAULT);
        actor2 = system.registerActor("actor2", CountingActor::new, ActorSettings.DEFAULT);
    }

    @TearDown
    public void tearDown() throws InterruptedException {
        system.shutDownAndWait();
    }

    @Benchmark
    @Threads(1)
    public void tellSingleActor() {
        actor1.tell(1);
    }

    @Benchmark
    @Threads(4)
    public void tellSingleActorContention() {
        actor1.tell(1);
    }

    @Benchmark
    @Threads(1)
    public void tellManyActors() {
        // This measures how fast we can distribute messages to many actors from one thread
        actor1.tell(1);
        actor2.tell(1);
    }

    @Benchmark
    public void askSingleActor(Blackhole bh) throws Exception {
        CompletableFuture<Integer> future = actor1.ask("GET");
        bh.consume(future.get());
    }

    @Benchmark
    public void tellTwoActors() {
        actor1.tell(1);
        actor2.tell(1);
    }

    @State(Scope.Benchmark)
    public static class ManyActorsState {
        public LintStoneSystem system;
        public List<LintStoneActorAccessor> actors;
        public final int actorCount = 1000;
        private final AtomicInteger index = new AtomicInteger(0);

        @Setup
        public void setup() {
            system = LintStoneSystemFactory.create();
            actors = new ArrayList<>();
            for (int i = 0; i < actorCount; i++) {
                actors.add(system.registerActor("many-" + i, CountingActor::new, ActorSettings.DEFAULT));
            }
        }

        @TearDown
        public void tearDown() throws InterruptedException {
            system.shutDownAndWait();
        }

        public void tellNext() {
            actors.get(Math.abs(index.getAndIncrement()) % actorCount).tell(1);
        }
    }

    @Benchmark
    public void tellRoundRobinManyActors(ManyActorsState state) {
        state.tellNext();
    }

    private static class CountingActor implements LintStoneActor {
        private int count = 0;

        @Override
        public void newMessageEvent(LintStoneMessageEventContext mec) {
            mec.inCase(Integer.class, (i, context) -> {
                count += i;
            }).inCase(String.class, (s, context) -> {
                if ("GET".equals(s)) {
                    context.reply(count);
                }
            });
        }
    }
}
