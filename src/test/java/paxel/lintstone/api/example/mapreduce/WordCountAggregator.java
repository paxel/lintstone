package paxel.lintstone.api.example.mapreduce;

import paxel.lintstone.api.LintStoneActor;
import paxel.lintstone.api.LintStoneMessageEventContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Aggregates word counts from multiple mappers.
 */
public class WordCountAggregator implements LintStoneActor {
    private final Map<String, Long> totalCounts = new HashMap<>();

    @Override
    public void newMessageEvent(LintStoneMessageEventContext mec) {
        mec.inCase(Map.class, (counts, ctx) -> {
            @SuppressWarnings("unchecked")
            Map<String, Long> partial = (Map<String, Long>) counts;
            partial.forEach((word, count) -> 
                totalCounts.put(word, totalCounts.getOrDefault(word, 0L) + count)
            );
        }).inCase(String.class, (msg, ctx) -> {
            if ("GET_RESULT".equals(msg)) {
                ctx.reply(new HashMap<>(totalCounts));
            }
        });
    }
}
