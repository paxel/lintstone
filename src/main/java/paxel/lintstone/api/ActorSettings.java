package paxel.lintstone.api;


import paxel.lintstone.impl.ActorSettingsBuilder;

/**
 * The actor settings for the creation of configured actors.
 */
public interface ActorSettings {

    /**
     * Non-blocking, unlimited and one by one processing Settings. Save but not optimal.
     */
    ActorSettings DEFAULT = ActorSettings.create().build();



    /**
     * The handler for uncaught exceptions in the actor.
     *
     * @return the error handler.
     */
    ErrorHandler errorHandler();

    /**
     * Create a builder to build an implementation of the Settings.
     *
     * @return a builder.
     */
    static ActorSettingsBuilder create() {
        return new ActorSettingsBuilder();
    }
}
