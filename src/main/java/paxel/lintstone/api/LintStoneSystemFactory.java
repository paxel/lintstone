package paxel.lintstone.api;

import paxel.lintstone.impl.ActorSystem;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Creates a LintStoneSystem. There can be multiple LintStoneSystems in
 * parallel. They don't interact.
 */
public class LintStoneSystemFactory {

    private LintStoneSystemFactory() {
    }

    /**
     * Creates a {@link LintStoneSystem} with default settings.
     *
     * @return the LintStoneSystem.
     */
    public static LintStoneSystem create() {
        return new ActorSystem();
    }


}
