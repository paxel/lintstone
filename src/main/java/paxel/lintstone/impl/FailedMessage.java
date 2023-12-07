package paxel.lintstone.impl;

import paxel.lintstone.api.LintStoneFailedMessage;

import java.util.Objects;

/**
 * Represents a failed message in the framework.
 * This class implements the LintStoneFailedMessage interface.
 * It contains the failed message, the cause of the failure, and the name of the actor where the failure occurred.
 */
public record FailedMessage(Object message, Throwable cause, String actorName) implements LintStoneFailedMessage {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FailedMessage that)) return false;

        if (!Objects.equals(message, that.message)) return false;
        if (!Objects.equals(cause, that.cause)) return false;
        return actorName.equals(that.actorName);
    }


    public String toString() {
        return "FailedMessage(message=" + this.message() + ", cause=" + this.cause() + ", actorName=" + this.actorName() + ")";
    }
}
