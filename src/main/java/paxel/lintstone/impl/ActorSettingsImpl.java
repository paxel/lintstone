package paxel.lintstone.impl;

import paxel.lintstone.api.ErrorHandler;
import paxel.lintstone.api.ActorSettings;

/**
 * Implementation of {@link ActorSettings}.
 *
 * @param errorHandler the error handler.
 */
public record ActorSettingsImpl(ErrorHandler errorHandler) implements ActorSettings {

}
