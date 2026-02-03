package paxel.lintstone.api;

import org.junit.jupiter.api.Test;
import paxel.lintstone.api.actors.SortNodeActor;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class ActorSortTest {

    @Test
    void sort() throws ExecutionException, InterruptedException {

        LintStoneSystem system = LintStoneSystemFactory.create();

        LintStoneActorAccessor root = system.registerActor("root", () -> new SortNodeActor("/"),  ActorSettings.DEFAULT);

        // send numbers to root actor. it will generate an actor system that resembles a simple binary tree
        Random random = new Random(1007);
        int number = 10_000;
        for (int i = 0; i < number; i++) {
            root.tellWithBackPressure(random.nextLong(), 100);
        }

        // request the sorted list from the root actor. It will also clean up the actor System
        List<Long> sorted = root.<List<Long>>ask("get").get();

        // check that the result is the same (is only true if no duplicates were created)
        assertThat(sorted).hasSize(number);

        // check that it's sorted
        assertThat(sorted).isSorted();

        // stop system
        system.shutDownNow();
    }
}
