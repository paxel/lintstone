package paxel.lintstone.api.example;

import paxel.lintstone.api.LintStoneActor;
import paxel.lintstone.api.LintStoneMessageEventContext;
import paxel.lintstone.api.example.utils.Snippets;

import java.util.Random;
import java.util.logging.Logger;

public class WordGeneratorActor implements LintStoneActor {

    private Logger log = Logger.getLogger(this.getClass().getName());

    private Snippets consonants;
    private Snippets vocals;
    private Random random;

    public record Init(Long seed) {
    }

    public record Request(int sylibls) {
    }

    public record Word(String value) {
    }

    @Override
    public void newMessageEvent(LintStoneMessageEventContext mec) {
        mec.inCase(Init.class, this::reinit)
                .inCase(Request.class, this::createWord)
                .otherwise(this::unknownMessage);
    }

    private void unknownMessage(Object o, LintStoneMessageEventContext lintStoneMessageEventContext) {
        log.severe("Unsupported message received: " + o.getClass() + ": <" + o + ">");
    }

    private void createWord(Request request, LintStoneMessageEventContext lintStoneMessageEventContext) {
        if (random == null)
            reinit(0L);
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < request.sylibls(); i++) {
            stringBuilder.append(consonants.get(random.nextDouble()));
            stringBuilder.append(vocals.get(random.nextDouble()));
        }
        lintStoneMessageEventContext.reply(new Word(stringBuilder.toString()));
    }

    private void reinit(long l) {
        consonants = new Snippets();
        consonants.add("q", random.nextInt());
        consonants.add("w", random.nextInt());
        consonants.add("r", random.nextInt());
        consonants.add("t", random.nextInt());
        consonants.add("z", random.nextInt());
        consonants.add("p", random.nextInt());
        consonants.add("s", random.nextInt());
        consonants.add("d", random.nextInt());
        consonants.add("f", random.nextInt());
        consonants.add("g", random.nextInt());
        consonants.add("h", random.nextInt());
        consonants.add("j", random.nextInt());
        consonants.add("k", random.nextInt());
        consonants.add("l", random.nextInt());
        consonants.add("x", random.nextInt());
        consonants.add("c", random.nextInt());
        consonants.add("v", random.nextInt());
        consonants.add("b", random.nextInt());
        consonants.add("n", random.nextInt());
        consonants.add("m", random.nextInt());
        consonants.add("sch", random.nextInt());
        consonants.add("ch", random.nextInt());
        consonants.add("sz", random.nextInt());
        consonants.add("st", random.nextInt());
        consonants.add("sp", random.nextInt());
        consonants.add("ck", random.nextInt());
        consonants.add("lg", random.nextInt());
        consonants.add("mm", random.nextInt());
        consonants.add("nn", random.nextInt());
        consonants.add("tt", random.nextInt());
        consonants.add("qu", random.nextInt());
        consonants.add("ff", random.nextInt());
        consonants.add("ll", random.nextInt());
        consonants.add("bb", random.nextInt());
        consonants.add("pp", random.nextInt());
        consonants.add("", random.nextInt());

        vocals = new Snippets();
        vocals.add("a", random.nextInt());
        vocals.add("e", random.nextInt());
        vocals.add("i", random.nextInt());
        vocals.add("o", random.nextInt());
        vocals.add("u", random.nextInt());
        vocals.add("y", random.nextInt());
        vocals.add("ee", random.nextInt());
        vocals.add("au", random.nextInt());
        vocals.add("eu", random.nextInt());
        vocals.add("ei", random.nextInt());
        vocals.add("ae", random.nextInt());
        vocals.add("oe", random.nextInt());
        vocals.add("ue", random.nextInt());
        vocals.add("ie", random.nextInt());
        vocals.add("ye", random.nextInt());
        vocals.add("", random.nextInt());
    }

    private void reinit(Init init, LintStoneMessageEventContext lintStoneMessageEventContext) {
        reinit(init.seed());
    }
}
