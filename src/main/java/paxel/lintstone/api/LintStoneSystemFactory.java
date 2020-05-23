package paxel.lintstone.api;

import paxel.lintstone.impl.DefaultLintStoneSystem;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Creates a LintStoneSystem. There can be multiple LintStoneSystems in
 * parallel. They don't interact.
 */
public class LintStoneSystemFactory {

    /**
     * Creates a {@link LintStoneSystem} with the given {@link ExecutorService}.
     *
     * @param executorService The service to execute the actors.
     * @return the LintStoneSystem.
     */
    public static LintStoneSystem create(ExecutorService executorService) {
        return new DefaultLintStoneSystem(executorService);
    }

    /**
     * This creates a {@link LintStoneSystem} for many Actors, that should not
     * use too many Threads.
     *
     * @param threads the number of threads that are allowed to run
     * concurrently.
     * @return the LintStoneSystem.
     */
    public static LintStoneSystem createLimitedThreadCount(int threads) {
        return new DefaultLintStoneSystem(Executors.newFixedThreadPool(threads));
    }

}
