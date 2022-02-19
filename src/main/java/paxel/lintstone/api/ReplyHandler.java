package paxel.lintstone.api;

/**
 * represents the handler for a reply. The mec object contains the actual reply and can be accessed the normal way.
 * You can NOT reply to this.
 */
public interface ReplyHandler {
    /**
     *
     * @param mec
     */
    void process(LintStoneMessageEventContext mec);
}
