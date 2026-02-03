package paxel.lintstone.api;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.*;

import org.junit.jupiter.api.Test;
import paxel.lintstone.api.actors.AdderActor;
import paxel.lintstone.api.actors.SumActor;
import paxel.lintstone.api.messages.EndMessage;

/**
 *
 */
public class LintStoneSystemTest {

    private Long result;
    final CountDownLatch latch = new CountDownLatch(1);

    public LintStoneSystemTest() {
    }

    @Test
    public void testSomeMethod() throws InterruptedException {
        LintStoneSystem system = LintStoneSystemFactory.create();
        LintStoneActorAccessor sumActor = system.registerActor("sumActor", () -> new SumActor(this::result),  ActorSettings.DEFAULT);

        Map<Integer, LintStoneActorAccessor> actors = new HashMap<>();
        for (int i = 1; i < 100000; i++) {
            int m = i % 30;
            actors.computeIfAbsent(m, val -> {
                        // create a new actor on the fly
                        final String name = "addActor" + val;
                        final LintStoneActorAccessor actor = system.registerActor(name, AdderActor::new,  ActorSettings.DEFAULT);
                        // register the actor at the sum actor
                        sumActor.tell(name);
                        // tell the adder his name.
                        actor.tell(name);
                        return actor;
                    })
                    // send the value to the actor
                    .tell(i);
        }

        // tell the adder, that it's finished
        final EndMessage endMessage = new EndMessage();
        for (Map.Entry<Integer, LintStoneActorAccessor> entry : actors.entrySet()) {
            entry.getValue().tell(endMessage);
        }

        // wait for the result
        latch.await();
        boolean unregisterActor = system.unregisterActor("sumActor");
        assertThat(unregisterActor, is(true));
        assertThat(result, is(4999950000L));
        system.shutDownAndWait(Duration.ofSeconds(5));
    }

    private void result(Long result) {
        this.result = result;
        latch.countDown();
    }
}
