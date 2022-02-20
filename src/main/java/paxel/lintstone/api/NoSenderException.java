package paxel.lintstone.api;

/**
 * Is thrown in case {@link LintStoneMessageEventContext#reply(java.lang.Object)
 * } was called, but no sender exists.
 */
public class NoSenderException extends RuntimeException {

    /**
     * Construct an Exception.
     *
     * @param message The message.
     */
    public NoSenderException(String message) {
        super(message);
    }

}
