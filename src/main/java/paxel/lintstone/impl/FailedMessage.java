package paxel.lintstone.impl;

import paxel.lintstone.api.LintStoneFailedMessage;

public class FailedMessage implements LintStoneFailedMessage {

    private final Object message;
    private final Throwable cause;
    private final String actorName;


    public FailedMessage(Object message, Throwable cause, String actorName) {
        this.message = message;
        this.cause = cause;
        this.actorName = actorName;
    }

    public Object getMessage() {
        return this.message;
    }

    public Throwable getCause() {
        return this.cause;
    }

    public String getActorName() {
        return this.actorName;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof FailedMessage)) return false;
        final FailedMessage other = (FailedMessage) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$message = this.getMessage();
        final Object other$message = other.getMessage();
        if (this$message == null ? other$message != null : !this$message.equals(other$message)) return false;
        final Object this$cause = this.getCause();
        final Object other$cause = other.getCause();
        if (this$cause == null ? other$cause != null : !this$cause.equals(other$cause)) return false;
        final Object this$actorName = this.getActorName();
        final Object other$actorName = other.getActorName();
        if (this$actorName == null ? other$actorName != null : !this$actorName.equals(other$actorName)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof FailedMessage;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $message = this.getMessage();
        result = result * PRIME + ($message == null ? 43 : $message.hashCode());
        final Object $cause = this.getCause();
        result = result * PRIME + ($cause == null ? 43 : $cause.hashCode());
        final Object $actorName = this.getActorName();
        result = result * PRIME + ($actorName == null ? 43 : $actorName.hashCode());
        return result;
    }

    public String toString() {
        return "FailedMessage(message=" + this.getMessage() + ", cause=" + this.getCause() + ", actorName=" + this.getActorName() + ")";
    }
}
