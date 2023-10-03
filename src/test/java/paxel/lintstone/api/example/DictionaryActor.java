package paxel.lintstone.api.example;

import paxel.lintstone.api.LintStoneActor;
import paxel.lintstone.api.LintStoneMessageEventContext;

public class DictionaryActor implements LintStoneActor {
    
    public record CreateDictionary(long seed){}
    @Override
    public void newMessageEvent(LintStoneMessageEventContext mec) {
        mec
                .inCase(CreateDictionary.class,this::initDictionary)
                .otherwise(this::otherwise);
    }

    private void initDictionary(CreateDictionary createDictionary, LintStoneMessageEventContext lintStoneMessageEventContext) {
        // the method that handles CreateDictionary messages
    }

    private void otherwise(Object o, LintStoneMessageEventContext lintStoneMessageEventContext) {
        // the method that handles all other message types
    }
}
