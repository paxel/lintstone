package paxel.lintstone.api;

import org.junit.jupiter.api.Test;
import paxel.lintstone.impl.ActorSystem;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class StaticDecisionTreeTest {

    @Test
    void testStaticDecisionTree() throws ExecutionException, InterruptedException {
        LintStoneSystem system = new ActorSystem();
        
        LintStoneActorAccessor actor = system.registerActor("staticActor", () -> new LintStoneActor() {
            @Override
            public void newMessageEvent(LintStoneMessageEventContext mec) {
                mec.inCase(String.class, (s, ctx) -> ctx.reply("Hello " + s))
                   .inCase(Integer.class, (i, ctx) -> ctx.reply(i * 2))
                   .otherwise((o, ctx) -> ctx.reply("Unknown"));
            }
        }, ActorSettings.DEFAULT);

        assertThat(actor.<String>ask("World").get()).isEqualTo("Hello World");
        assertThat(actor.<Integer>ask(21).get()).isEqualTo(42);
        assertThat(actor.<String>ask(1.0).get()).isEqualTo("Unknown");

        system.shutDownNow();
    }
}
