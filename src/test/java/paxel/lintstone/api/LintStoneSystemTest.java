package paxel.lintstone.api;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

/**
 *
 */
public class LintStoneSystemTest {

    private Long result;
    CountDownLatch latch = new CountDownLatch(1);

    public LintStoneSystemTest() {
    }

    @Test
    public void testSomeMethod() throws InterruptedException {
        LintStoneSystem system = LintStoneSystemFactory.createLimitedThreadCount(5);
        LintStoneActorAccess sumActor = system.registerActor("sumActor", () -> new SumActor(this::result), Optional.empty());

        Map<Integer, LintStoneActorAccess> actors = new HashMap<>();
        for (int i = 1; i < 100000; i++) {
            int m = i % 30;
            actors.computeIfAbsent(m, val -> {
                // create a new actor on the fly
                final String name = "addActor" + val;
                final LintStoneActorAccess actor = system.registerActor(name, AdderActor::new, Optional.empty());
                // register the actor at the sum actor
                sumActor.send(name);
                return actor;
            })
                    // send the value to the actor
                    .send(i);
        }

        // tell the adder, that it's finished
        final EndMessage endMessage = new EndMessage();
        for (Map.Entry<Integer, LintStoneActorAccess> entry : actors.entrySet()) {
            entry.getValue().send(endMessage);
        }

        // wait for the result
        latch.await();
        assertThat(result, is(4999950000L));
        system.shutDownAndWait(Duration.ofSeconds(5));
    }

    private void result(Long result) {
        this.result = result;
        latch.countDown();
    }
}
