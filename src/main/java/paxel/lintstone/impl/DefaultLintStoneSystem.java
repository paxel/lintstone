package paxel.lintstone.impl;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import paxel.lintstone.api.LintStoneActorAccess;
import paxel.lintstone.api.LintStoneActorFactory;
import paxel.lintstone.api.LintStoneSystem;

/**
 *
 */
public class DefaultLintStoneSystem implements LintStoneSystem {

    public DefaultLintStoneSystem(ExecutorService executorService) {
    }

    @Override
    public LintStoneActorAccess registerActor(String name, LintStoneActorFactory factory, Optional<Object> initMessage) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void shutDown() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void shutDownAndWait() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean shutDownAndWait(Duration timeout) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void shutDownNow() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean unregisterActor(String name) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
