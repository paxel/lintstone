package paxel.lintstone.api.actors;

import paxel.lintstone.api.LintStoneActor;
import paxel.lintstone.api.LintStoneMessageEventContext;
import paxel.lintstone.api.messages.EndMessage;

public class CharCount implements LintStoneActor {
    private int count;

    @Override
    public void newMessageEvent(LintStoneMessageEventContext mec) {
        mec
                .inCase(String.class, this::handleString)
                .inCase(EndMessage.class, this::handleEnd);
    }

    private void handleString(String txt, LintStoneMessageEventContext m) {
        this.count += txt.replaceAll(" ", "").length();
    }

    private void handleEnd(EndMessage dmg, LintStoneMessageEventContext askContext) {
        askContext.reply(count);
        askContext.unregister();
    }
}
