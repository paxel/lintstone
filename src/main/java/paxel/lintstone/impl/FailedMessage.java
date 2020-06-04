package paxel.lintstone.impl;

import paxel.lintstone.api.LintStoneFailedMessage;

/**
 *
 */
public class FailedMessage implements LintStoneFailedMessage {

    private final Object message;
    private final Throwable cause;
    private final String actorName;

    public FailedMessage(Object message, Throwable cause, String actorName) {
        this.message = message;
        this.cause = cause;
        this.actorName = actorName;
    }

    @Override
    public Object getMessage() {
        return message;
    }

    @Override
    public Throwable getCause() {
        return cause;
    }

    @Override
    public String getActorName() {
        return actorName;
    }

}
