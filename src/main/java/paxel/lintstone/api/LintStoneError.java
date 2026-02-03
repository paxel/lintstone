package paxel.lintstone.api;

/**
 * Types of errors that can occur in the LintStone actor system.
 */
public enum LintStoneError {
    /**
     * Occurs when an actor fails while processing a message.
     */
    MESSAGE_PROCESSING_FAILED,
    /**
     * Occurs when an actor fails while processing a reply.
     */
    REPLY_PROCESSING_FAILED,
    /**
     * Occurs when an unexpected error happens in the system.
     */
    UNEXPECTED_ERROR
}
