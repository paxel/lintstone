package paxel.lintstone.impl;

import paxel.bulkexecutor.ErrorHandler;
import paxel.lintstone.api.ActorSettings;

public record ActorSettingsImpl(int batch, ErrorHandler errorHandler) implements ActorSettings {

}
