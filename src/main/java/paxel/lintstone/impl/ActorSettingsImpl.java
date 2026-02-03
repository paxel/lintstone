package paxel.lintstone.impl;

import lombok.NonNull;
import paxel.lintstone.api.ErrorHandler;
import paxel.lintstone.api.ActorSettings;

/**
 * Implementation of {@link ActorSettings}.
 *
 * @param errorHandler the error handler.
 */
public record ActorSettingsImpl(@NonNull ErrorHandler errorHandler, int queueLimit) implements ActorSettings {

}
