package paxel.lintstone.api.example;

import paxel.lintstone.api.*;

import java.util.Dictionary;

public class Verboseify {

    public static void main(String[] args) {
        LintStoneSystem lintStoneSystem = LintStoneSystemFactory.create();
        String name = "dictionary"; // name of the actor in the system
        LintStoneActorFactory factory = DictionaryActor::new; // creates an actor
        ActorSettings settings = ActorSettings.DEFAULT; // Default Settings
        Object initMessage = null; // init message or null
        LintStoneActorAccessor dictionary = lintStoneSystem.registerActor(name, factory, settings, initMessage);
    }
}
