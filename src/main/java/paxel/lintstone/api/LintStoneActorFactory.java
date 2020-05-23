package paxel.lintstone.api;

/**
 * A factory for creating Actors.
 */
public interface LintStoneActorFactory {

    /**
     * Creates an actor.
     *
     * @return the new actor.
     */
    LintStoneActor create();
}
