package paxel.lintstone.api;

/**
 * The interface for a LintStone actor.
 */
@FunctionalInterface
public interface LintStoneActor {

    /**
     * This method is called once when the actor is created to initialize its message handling logic.
     * The {@link LintStoneMessageEventContext} should be used to define a decision tree using
     * {@code inCase} and {@code otherwise} handlers. These handlers will then be used for every
     * message the actor receives.
     *
     * It is not safe to keep a reference to the mec instance. It is safe to store the
     * LintStoneActorAccessor instances, but those will become invalid if the Actor with the given
     * name is unregistered.
     *
     * @param mec The context used to register message handlers and access the Actor system.
     */
    void newMessageEvent(LintStoneMessageEventContext mec);
}
