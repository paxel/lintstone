package paxel.lintstone.api;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

// only fork 1 JVM per benchmark
@Fork(1)
// 5 times 2 second warmup per benchmark
@Warmup(iterations = 5, time = 10)
// 5 times 2 second measurment per benchmark
@Measurement(iterations = 5, time = 10)
// in micros
@OutputTimeUnit(TimeUnit.SECONDS)
public class JmhTest {

    private static final String TEST = "Test";

    @Benchmark
    @OperationsPerInvocation(1_000)
    public void run_____1_Actors(Blackhole blackhole) throws InterruptedException, ExecutionException {
        int threads = 1;
        int actorCount = 1;
        int messages = 1_000;

        run(actorCount, messages, LintStoneSystemFactory.create(), blackhole);
    }

    @Benchmark
    @OperationsPerInvocation(2_000)
    public void run_____2_Actors(Blackhole blackhole) throws InterruptedException, ExecutionException {
        int threads = 1;
        int actorCount = 2;
        int messages = 2_000;

        run(actorCount, messages, LintStoneSystemFactory.create(), blackhole);
    }

    @Benchmark
    @OperationsPerInvocation(10_000)
    public void run____10_Actors(Blackhole blackhole) throws InterruptedException, ExecutionException {
        int actorCount = 10;
        int messages = 10_000;

        run(actorCount, messages, LintStoneSystemFactory.create(), blackhole);
    }


    @Benchmark
    @OperationsPerInvocation(20_000)
    public void run____20_Actors(Blackhole blackhole) throws InterruptedException, ExecutionException {
        int actorCount = 20;
        int messages = 20_000;

        run(actorCount, messages, LintStoneSystemFactory.create(), blackhole);
    }

    @Benchmark
    @OperationsPerInvocation(30_000)
    public void run____30_Actors(Blackhole blackhole) throws InterruptedException, ExecutionException {
        int actorCount = 30;
        int messages = 30_000;

        run(actorCount, messages, LintStoneSystemFactory.create(), blackhole);
    }

    @Benchmark
    @OperationsPerInvocation(999_000)
    public void run___999_Actors(Blackhole blackhole) throws InterruptedException, ExecutionException {
        int actorCount = 999;
        int messages = 999_000;

        run(actorCount, messages, LintStoneSystemFactory.create(), blackhole);
    }


    @Benchmark
    @OperationsPerInvocation(50_000_000)
    public void run_50000_Actors(Blackhole blackhole) throws InterruptedException, ExecutionException {
        int actorCount = 50_000;
        int messages = 50_000_000;

        run(actorCount, messages, LintStoneSystemFactory.create(), blackhole);
    }


    private void run(int actorCount, int messages, LintStoneSystem system, Blackhole blackhole) throws InterruptedException, UnregisteredRecipientException, ExecutionException {
        List<LintStoneActorAccessor> actors = new ArrayList<>();
        for (int i = 0; i < actorCount; i++) {
            actors.add(system.registerActor(TEST + i, MessageActor::new, ActorSettings.DEFAULT));
        }
        for (int i = 0; i < messages; i++) {
            actors.get(i % actorCount).send(i);
        }
        for (int i = 0; i < actorCount; i++) {
            // finish the actors
            Integer end = actors.get(i).<Integer>ask("END").get();
            blackhole.consume(end);
        }
        system.shutDownAndWait();
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(JmhTest.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }


    private static class MessageActor implements LintStoneActor {

        private int sum;

        @Override
        public void newMessageEvent(LintStoneMessageEventContext mec) {
            mec.inCase(Integer.class, (a, b) -> {
                        // add the number
                        try {
                            // simulate some load. Only adding a number will never be a reason for an actor ;)
                            Thread.sleep(0, 666);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        sum += a;
                    }
            ).inCase(String.class, (name, reply) -> {
                // notify to the given name, the sum
                reply.reply(sum);
                // and kill yourself
                reply.unregister();
            }).otherwise((a, b) -> System.err.println("unknown message: " + a));
        }
    }

}
