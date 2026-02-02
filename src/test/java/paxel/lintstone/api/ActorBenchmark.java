package paxel.lintstone.api;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 3, time = 2)
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
    public void tellSingleActor() {
        actor1.tell(1);
    }

    @Benchmark
    @Threads(4)
    public void tellSingleActorContention() {
        actor1.tell(1);
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
