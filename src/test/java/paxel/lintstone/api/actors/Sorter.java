package paxel.lintstone.api.actors;

import paxel.lintstone.api.LintStoneActor;
import paxel.lintstone.api.LintStoneMessageEventContext;
import paxel.lintstone.api.messages.EndMessage;

import java.util.*;
import java.util.stream.Collectors;

public class Sorter implements LintStoneActor {
    final Set<String> words = new HashSet<>();

    @Override
    public void newMessageEvent(LintStoneMessageEventContext mec) {
        mec
                .inCase(String.class, this::handle)
                .inCase(EndMessage.class, this::handle);
    }

    private void handle(String txt, LintStoneMessageEventContext m) {
        words.addAll(Arrays.stream(txt.trim().split(" "))
                .filter(f -> !f.trim().isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toList()));
    }

    private void handle(EndMessage dmg, LintStoneMessageEventContext askContext) {
        ArrayList<String> list = new ArrayList<>(words);
        Collections.sort(list);
        askContext.reply(String.join(",", list));
    }
}
