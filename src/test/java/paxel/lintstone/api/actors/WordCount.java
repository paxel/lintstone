package paxel.lintstone.api.actors;

import paxel.lintstone.api.LintStoneActor;
import paxel.lintstone.api.LintStoneMessageEventContext;
import paxel.lintstone.api.messages.EndMessage;

import java.util.Arrays;

public class WordCount implements LintStoneActor {
    private int count;

    @Override
    public void newMessageEvent(LintStoneMessageEventContext mec) {
        mec
                .inCase(String.class, this::handle)
                .inCase(EndMessage.class, this::handle);
    }

    private void handle(String txt, LintStoneMessageEventContext m) {
        int length = (int) Arrays.stream(txt.trim().split(" ")).filter(f -> !f.trim().isEmpty()).count();
        this.count += length;
    }

    private void handle(EndMessage dmg, LintStoneMessageEventContext askContext) {
        askContext.reply(count);
        askContext.unregister();
    }
}
