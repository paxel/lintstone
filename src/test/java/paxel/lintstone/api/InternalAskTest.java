package paxel.lintstone.api;

import org.junit.Test;
import paxel.lintstone.api.actors.CharCount;
import paxel.lintstone.api.actors.Distributor;
import paxel.lintstone.api.actors.SorterActor;
import paxel.lintstone.api.actors.WordCount;
import paxel.lintstone.api.messages.EndMessage;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

        LintStoneActorAccessor dist = system.registerActor("dist", Distributor::new,  ActorSettings.DEFAULT);
        system.registerActor("wordCount", WordCount::new,  ActorSettings.DEFAULT);
        system.registerActor("charCount", CharCount::new,  ActorSettings.DEFAULT);
        system.registerActor("sorter", SorterActor::new, ActorSettings.DEFAULT);

        LintStoneSystem s = LintStoneSystemFactory.create();
        LintStoneActorAccessor syncedOut = s.registerActor("out", () -> mec -> mec.otherwise((o, m)->System.out.println(o)),  ActorSettings.DEFAULT);

        for (String text : data) {
            dist.tell(text);
        }
        dist.ask(new EndMessage(), replyMec -> replyMec.inCase(String.class, (reply, ignored) -> result.complete(reply)));

        Object v = result.get(1, TimeUnit.MINUTES);

        // this should be repeatable correct. because the messages are processed in correct order and the asks will be the last one
        assertThat(v, is("72 words, 403 letters, 15,am,antrag,auf,berlin,beschwörend,chef,choreographie,debatte,dem,denn,der,die,digitalen,doch,eigentlich,eine,einem,einen,einzelne,ende,es,fast,formulierungen,für,geht,generalsekretärin,hanau,hier,in,jusos,klingt,krönungsmesse,kurz,kutschaty,lüders,mehr,nach,nadja,parteitag,rassismus,sagt,samstagnachmittag,schon,sein,sie,sind,soll,spd,sprengen,stellen,taz,terroranschlag,thomas,uhr,um,und,viel,wahlparteitag,wir,zeitplan,zu,zwingen"));
        assertThat(dist.getQueuedMessagesAndReplies(), is(0));
        assertThat(dist.getProcessedMessages(), is(8L)); //7 Strings 1 ask
        assertThat(dist.getProcessedReplies(), is(4L)); // processed the ask reply for external call and 3 replies of the other actors
        system.shutDown();
    }


}
