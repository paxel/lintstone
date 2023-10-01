package paxel.lintstone.api.actors;

import paxel.lintstone.api.LintStoneActor;
import paxel.lintstone.api.LintStoneMessageEventContext;
import paxel.lintstone.api.messages.EndMessage;

import java.text.MessageFormat;
import java.util.concurrent.CompletableFuture;

public class Distributor implements LintStoneActor {

    @Override
    public void newMessageEvent(LintStoneMessageEventContext mec) {
        mec
                .inCase(String.class, this::send)
                .inCase(EndMessage.class, this::handleEnd);
    }


    private void send(String txt, LintStoneMessageEventContext mec) {
        mec.send("wordCount", txt);
        mec.send("charCount", txt);
        mec.send("sorter", txt);
    }

    private void handleEnd(EndMessage dmg, LintStoneMessageEventContext askContext) {
        CompletableFuture<String> sort = new CompletableFuture<>();
// each of these completes will be called in the thread context of this actor
        CompletableFuture<Integer> words = askContext.ask("wordCount", new EndMessage());
        CompletableFuture<Integer> chars = askContext.ask("charCount", new EndMessage());
        // other way to ask
        askContext.ask("sorter", new EndMessage(), c -> c.inCase(String.class, (r, replyContext) -> sort.complete(r)));

// when the last reply comes, the reply of the external ask is fulfilled.
        CompletableFuture.allOf(words, chars, sort).thenApply(x -> {
            try {
                askContext.reply(MessageFormat.format("{0} words, {1} letters, {2}", words.get(), chars.get(), sort.get()));
            } catch (Exception e) {
                askContext.reply(e.getMessage());
            }
            return null;
        });
    }
}
