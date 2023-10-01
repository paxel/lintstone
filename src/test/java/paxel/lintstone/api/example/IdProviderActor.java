package paxel.lintstone.api.example;

import paxel.lintstone.api.LintStoneActor;
import paxel.lintstone.api.LintStoneMessageEventContext;

import java.util.*;
import java.util.logging.Logger;

public class IdProviderActor implements LintStoneActor {

    private Logger log = Logger.getLogger(this.getClass().getName());
    private int nextId = 0;
    private LinkedList<Integer> removed = new LinkedList<>();
    private Map<String, Integer> ids = new HashMap<>();

    public record AddText(String value) {
    }

    public record RemoveText(String value) {
    }

    public record Id(Integer value) {
    }

    public record SizeRequest() {

    }

    public record SizeResponse(Integer current, Integer peak) {

    }

    @Override
    public void newMessageEvent(LintStoneMessageEventContext mec) {
        mec.inCase(AddText.class, this::cacheText)
                .inCase(RemoveText.class, this::removeText)
                .inCase(SizeRequest.class, this::size)
                .otherwise(this::unkownMessage);
    }

    private void size(SizeRequest sizeRequest, LintStoneMessageEventContext lintStoneMessageEventContext) {
        lintStoneMessageEventContext.reply(new SizeResponse(ids.size(), nextId));
    }

    private void unkownMessage(Object o, LintStoneMessageEventContext lintStoneMessageEventContext) {
        log.severe("Unsupported message received: " + o.getClass() + ": <" + o + ">");
    }

    private void removeText(RemoveText removeText, LintStoneMessageEventContext lintStoneMessageEventContext) {
        // removes the text from the cache and adds the ID for re-usage.
        Integer remove = ids.remove(removeText.value());
        if (remove != null)
            removed.add(remove);
    }

    private void cacheText(AddText text, LintStoneMessageEventContext lintStoneMessageEventContext) {
        Integer i = ids.get(text.value());
        if (i != null)
            // we know this one already
            lintStoneMessageEventContext.reply(new Id(i));
        else {
            Integer poll = removed.poll();
            if (poll != null) {
                // we can reuse an old ID
                cacheEntry(text, lintStoneMessageEventContext, poll);
            } else {
                // create a new ID
                cacheEntry(text, lintStoneMessageEventContext, nextId);
                nextId++;
            }
        }
    }

    private void cacheEntry(AddText text, LintStoneMessageEventContext lintStoneMessageEventContext, Integer poll) {
        ids.put(text.value(), poll);
        lintStoneMessageEventContext.reply(new Id(poll));
    }
}
