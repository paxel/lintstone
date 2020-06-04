package paxel.lintstone.api;

/**
 *
 */
public interface LintStoneFailedMessage {

    Object getMessage();

    Throwable getCause();

    String getActorName();
}
