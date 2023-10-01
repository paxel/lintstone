package paxel.lintstone.api;

import org.junit.Test;

import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 *
 */
public class InternalAskTest {

    final CompletableFuture result = new CompletableFuture();
    private final static String[] data = {
            "BERLIN taz Wir sind hier auf einem Wahlparteitag sagt Nadja Lüders ",
            "SPD Generalsekretärin kurz nach 15 Uhr am Samstagnachmittag ",
            "Es klingt fast beschwörend ",
            "Denn eigentlich soll der Parteitag die Krönungsmesse für SPD Chef Thomas Kutschaty  schon zu Ende sein ",
            "Doch die Jusos zwingen dem digitalen Parteitag eine Rassismus Debatte auf die Zeitplan und Choreographie sprengen",
            "Sie stellen einen Antrag zu dem Terroranschlag in Hanau ",
            "Es geht um einzelne Formulierungen und viel mehr"
    };

    public InternalAskTest() {
    }

    @Test
    public void testAskExternal() throws InterruptedException, ExecutionException, TimeoutException {
        LintStoneSystem system = LintStoneSystemFactory.create();
        // the entry actor is limited to 1 message at the time input queue
        // just to test that the messages are added even so we send faster than the actor can process (creating backpressure)
        LintStoneActorAccessor dist = system.registerActor("dist", Distributor::new,  ActorSettings.DEFAULT);
        system.registerActor("wordCount", WordCount::new,  ActorSettings.DEFAULT);
        system.registerActor("charCount", CharCount::new,  ActorSettings.DEFAULT);
        system.registerActor("sorter", Sorter::new, ActorSettings.DEFAULT);

        LintStoneSystem s = LintStoneSystemFactory.create();
        LintStoneActorAccessor syncedOut = s.registerActor("out", () -> mec -> mec.otherwise((o, m)->System.out.println(o)),  ActorSettings.DEFAULT);

        for (String text : data) {
            dist.send(text);
        }
        dist.ask(new EndMessage(), replyMec -> replyMec.inCase(String.class, (reply, ignored) -> {
            result.complete(reply);
        }));

        Object v = result.get(1, TimeUnit.MINUTES);

        // this should be repeatable correct. because the messages are processed in correct order and the asks will be the last one
        assertThat(v, is("72 words, 403 letters, 15,am,antrag,auf,berlin,beschwörend,chef,choreographie,debatte,dem,denn,der,die,digitalen,doch,eigentlich,eine,einem,einen,einzelne,ende,es,fast,formulierungen,für,geht,generalsekretärin,hanau,hier,in,jusos,klingt,krönungsmesse,kurz,kutschaty,lüders,mehr,nach,nadja,parteitag,rassismus,sagt,samstagnachmittag,schon,sein,sie,sind,soll,spd,sprengen,stellen,taz,terroranschlag,thomas,uhr,um,und,viel,wahlparteitag,wir,zeitplan,zu,zwingen"));
        assertThat(dist.getQueuedMessagesAndReplies(), is(0));
        assertThat(dist.getProcessedMessages(), is(8L)); //7 Strings 1 ask
        assertThat(dist.getProcessedReplies(), is(4L)); // processed the ask reply for external call and 3 replies of the other actors
        system.shutDown();
    }


    private static class Distributor implements LintStoneActor {
        @Override
        public void newMessageEvent(LintStoneMessageEventContext mec) {
            mec.inCase(String.class, this::send).inCase(EndMessage.class, (dmg, askContext) -> {
                CompletableFuture<Integer> words = new CompletableFuture<>();
                CompletableFuture<Integer> chars = new CompletableFuture<>();
                CompletableFuture<String> sort = new CompletableFuture<>();
                // each of these completes will be called in the thread context of this actor
                mec.ask("wordCount", new EndMessage(), c -> c.inCase(Integer.class, (r, replyContext) -> words.complete(r)));
                mec.ask("charCount", new EndMessage(), c -> c.inCase(Integer.class, (r, replyContext) -> chars.complete(r)));
                mec.ask("sorter", new EndMessage(), c -> c.inCase(String.class, (r, replyContext) -> sort.complete(r)));

                // when the last reply comes, the reply of the external ask is fulfilled.
                CompletableFuture.allOf(words, chars, sort).thenApply(x -> {
                    try {
                        askContext.reply(MessageFormat.format("{0} words, {1} letters, {2}", words.get(), chars.get(), sort.get()));
                    } catch (Exception e) {
                        askContext.reply(e.getMessage());
                    }
                    return null;
                });
            });
        }


        private void send(String txt, LintStoneMessageEventContext mec) {
            mec.send("wordCount", txt);
            mec.send("charCount", txt);
            mec.send("sorter", txt);
        }
    }

    private static class WordCount implements LintStoneActor {
        private int count;

        @Override
        public void newMessageEvent(LintStoneMessageEventContext mec) {
            mec.inCase(String.class, (txt, m) -> {
                int length = (int) Arrays.stream(txt.trim().split(" ")).filter(f->!f.trim().isEmpty()).count();
                this.count += length;
            }).inCase(EndMessage.class, (dmg, askContext) -> {
                askContext.reply(count);
                askContext.unregister();
            });
        }
    }

    private static class CharCount implements LintStoneActor {
        private int count;

        @Override
        public void newMessageEvent(LintStoneMessageEventContext mec) {
            mec.inCase(String.class, (txt, m) -> this.count += txt.replaceAll(" ", "").length()).inCase(EndMessage.class, (dmg, askContext) -> {
                askContext.reply(count);
                askContext.unregister();
            });
        }
    }

    private static class Sorter implements LintStoneActor {
        final Set<String> words = new HashSet<>();

        @Override
        public void newMessageEvent(LintStoneMessageEventContext mec) {
            mec.inCase(String.class, (txt, m) -> words.addAll(Arrays.stream(txt.trim().split(" ")).filter(f->!f.trim().isEmpty()).map(String::toLowerCase).collect(Collectors.toList()))).inCase(EndMessage.class, (dmg, askContext) -> {
                ArrayList<String> list = new ArrayList<>(words);
                Collections.sort(list);
                askContext.reply(String.join(",", list));
            });
        }
    }
}
