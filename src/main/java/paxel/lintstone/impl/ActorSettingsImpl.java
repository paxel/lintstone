package paxel.lintstone.impl;

import paxel.lintstone.api.ErrorHandler;
import paxel.lintstone.api.ActorSettings;

public record ActorSettingsImpl(ErrorHandler errorHandler) implements ActorSettings {

}
