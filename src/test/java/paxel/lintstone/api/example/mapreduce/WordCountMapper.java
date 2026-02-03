package paxel.lintstone.api.example.mapreduce;

import paxel.lintstone.api.LintStoneActor;
import paxel.lintstone.api.LintStoneMessageEventContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Counts occurrences of words in a chunk of lines.
 */
public class WordCountMapper implements LintStoneActor {
    @Override
    public void newMessageEvent(LintStoneMessageEventContext mec) {
        mec.inCase(String.class, (line, ctx) -> {
            Map<String, Long> counts = new HashMap<>();
            String[] words = line.toLowerCase().split("\\W+");
            for (String word : words) {
                if (!word.isEmpty()) {
                    counts.put(word, counts.getOrDefault(word, 0L) + 1);
                }
            }
            // Send partial results to the aggregator
            ctx.tell("aggregator", counts);
        });
    }
}
