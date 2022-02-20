package paxel.lintstone.api;

/**
 * represents the handler for a reply. The mec object contains the actual reply and can be accessed the normal way.
 * You can NOT reply to this.
 */
@FunctionalInterface
public interface ReplyHandler {
    /**
     * Processes the reply encapsulated in the context.
     *
     * @param mec The message context.
     */
    void process(LintStoneMessageEventContext mec);
}
