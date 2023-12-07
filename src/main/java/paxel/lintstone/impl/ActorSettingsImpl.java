package paxel.lintstone.impl;

import paxel.lintstone.api.ErrorHandler;
import paxel.lintstone.api.ActorSettings;

/**
 * Implementation of the {@link ActorSettings} interface. Represents the actor settings for the creation of configured actors.
 */
public record ActorSettingsImpl(ErrorHandler errorHandler) implements ActorSettings {

}
