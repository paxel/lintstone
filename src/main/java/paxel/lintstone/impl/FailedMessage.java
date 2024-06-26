package paxel.lintstone.impl;

import paxel.lintstone.api.LintStoneFailedMessage;

import java.util.Objects;

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
