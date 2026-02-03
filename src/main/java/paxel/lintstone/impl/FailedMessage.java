package paxel.lintstone.impl;

import lombok.NonNull;
import paxel.lintstone.api.LintStoneFailedMessage;

/**
 * Implementation of {@link LintStoneFailedMessage}.
 *
 * @param message   the failed message.
 * @param cause     the cause of the failure.
 * @param actorName the name of the failing actor.
 */
public record FailedMessage(@NonNull Object message, @NonNull Throwable cause, @NonNull String actorName) implements LintStoneFailedMessage {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FailedMessage that)) return false;

        if (!message.equals(that.message)) return false;
        if (!cause.equals(that.cause)) return false;
        return actorName.equals(that.actorName);
    }


    public String toString() {
        return "FailedMessage(message=" + this.message() + ", cause=" + this.cause() + ", actorName=" + this.actorName() + ")";
    }
}
