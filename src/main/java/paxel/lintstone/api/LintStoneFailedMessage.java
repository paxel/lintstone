package paxel.lintstone.api;

/**
 * Represents an error reply by the Framework.
 * It is good practice having an inCase for this type.
 */
public interface LintStoneFailedMessage {

    /**
     * Retrieve the failed message.
     *
     * @return the failed message
     */
    Object getMessage();

    /**
     * Retrieve the cause of the failure.
     *
     * @return the cause.
     */
    Throwable getCause();

    /**
     * Retrieve the name of the failing actor.
     *
     * @return
     */
    String getActorName();
}
