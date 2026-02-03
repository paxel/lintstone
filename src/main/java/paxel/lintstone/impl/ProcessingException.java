package paxel.lintstone.impl;

import lombok.Getter;
import lombok.NonNull;
import paxel.lintstone.api.LintStoneError;

/**
 * Internal exception used to pass error context to the sequential processor.
 */
@Getter
class ProcessingException extends RuntimeException {
    private final @NonNull LintStoneError error;
    private final @NonNull String description;

    ProcessingException(@NonNull LintStoneError error, @NonNull String description, @NonNull Throwable cause) {
        super(cause);
        this.error = error;
        this.description = description;
    }
}
