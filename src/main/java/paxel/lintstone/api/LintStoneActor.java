package paxel.lintstone.api;

@FunctionalInterface
public interface LintStoneActor {

    /**
     * This is the only method that an actor needs to implement. The
     * {@link LintStoneMessageEventContext} is used to handle the message, reply
     * to the sender, send messages to other actors and also register and
     * unregister other actors. It is not safe to keep a reference to the mec
     * instance, because it might or might not be created per message. It is
     * safe to store the LintStoneActorAccess instances, but those will become
     * invalid if the Actor with the given name is unregistered.
     *
     * @param mec The context, containing the message and access to the Actor
     * system.
     */
    void newMessageEvent(LintStoneMessageEventContext mec);
}
