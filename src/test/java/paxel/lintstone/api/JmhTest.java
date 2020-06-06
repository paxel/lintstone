package paxel.lintstone.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

// only fork 1 JVM per benchmark
@Fork(1)
// 5 times 2 second warmup per benchmark
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
// 5 times 2 second measurment per benchmark
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
// in micros
@OutputTimeUnit(TimeUnit.SECONDS)
public class JmhTest {

    private static final String TEST = "Test";

    @Benchmark
    @OperationsPerInvocation(100000)
    public void run001ActorOn001Thread() throws InterruptedException {
        int threads = 1;
        int actorCount = 1;
        int messages = 100_000;

        run(threads, actorCount, messages);
    }

    @Benchmark
    @OperationsPerInvocation(100000)
    public void run002ActorOn001Thread() throws InterruptedException {
        int threads = 1;
        int actorCount = 2;
        int messages = 100_000;

        run(threads, actorCount, messages);
    }

    @Benchmark
    @OperationsPerInvocation(100000)
    public void run010ActorOn001Thread() throws InterruptedException {
        int threads = 1;
        int actorCount = 10;
        int messages = 100_000;

        run(threads, actorCount, messages);
    }

    @Benchmark
    @OperationsPerInvocation(100000)
    public void run010ActorOn010Thread() throws InterruptedException {
        int threads = 10;
        int actorCount = 10;
        int messages = 100_000;

        run(threads, actorCount, messages);
    }

    @Benchmark
    @OperationsPerInvocation(100000)
    public void run020ActorOn020Thread() throws InterruptedException {
        int threads = 20;
        int actorCount = 20;
        int messages = 100_000;

        run(threads, actorCount, messages);
    }

    @Benchmark
    @OperationsPerInvocation(100000)
    public void run030ActorOn020Thread() throws InterruptedException {
        int threads = 20;
        int actorCount = 30;
        int messages = 100_000;

        run(threads, actorCount, messages);
    }

    @Benchmark
    @OperationsPerInvocation(100000)
    public void run100ActorOn001Threads() throws InterruptedException {
        int threads = 10;
        int actorCount = 10000;
        int messages = 100_000;

        run(threads, actorCount, messages);
    }

    private void run(int threads, int actorCount, int messages) throws InterruptedException, UnregisteredRecipientException {
        CountDownLatch latch = new CountDownLatch(threads);
        LintStoneSystem system = LintStoneSystemFactory.createLimitedThreadCount(threads);
        system.registerActor("END", () -> new EndActor(latch), Optional.empty());
        List<LintStoneActorAccess> actors = new ArrayList<>();
        for (int i = 0; i < actorCount; i++) {
            actors.add(system.registerActor(TEST + i, () -> new MessageActor(), Optional.empty()));
        }
        for (int i = 0; i < messages; i++) {
            actors.get(i % actorCount).send(i);
        }
        for (int i = 0; i < actorCount; i++) {
            // finish the actors
            actors.get(i).send("END");
        }
        latch.await();
        system.shutDownAndWait();
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(JmhTest.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }

    private static class EndActor implements LintStoneActor {

        private final CountDownLatch latch;

        public EndActor(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void newMessageEvent(LintStoneMessageEventContext mec) {
            latch.countDown();
        }
    }

    private static class MessageActor implements LintStoneActor {

        private int sum;

        @Override
        public void newMessageEvent(LintStoneMessageEventContext mec) {
            mec.inCase(Integer.class, (a, b) -> {
                // add the number
                sum += a;
            }
            ).inCase(String.class, (a, b) -> {
                // notify to the given name, the sum
                b.send(a, sum);
                // and kill yourself
                b.unregister();
            }).otherwise((a, b) -> {
                System.err.println("unknown message: " + a);
            });
        }
    }

}
