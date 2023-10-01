package paxel.lintstone.api;

import org.junit.Test;
import paxel.lintstone.api.actors.Md5Actor;
import paxel.lintstone.api.messages.EndMessage;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 *
 */
public class ExternalAskTest {

    final CountDownLatch latch = new CountDownLatch(1);


    @Test
    public void testAskExternalFuture() throws InterruptedException, ExecutionException {
        LintStoneSystem system = LintStoneSystemFactory.create();
        LintStoneActorAccessor md5 = system.registerActor("md5", Md5Actor::new, ActorSettings.DEFAULT);

        md5.send("This is my test string");
        for (int i = 0; i < 1000; i++) {
            md5.send(ByteBuffer.wrap(new byte[i]));
        }
        String result = md5.<String>ask(new EndMessage()).get();

        // this should be repeatable correct. because the messages are processed in correct order and the ask will be the last one
        assertThat(result, is("993e7b2144d8c8a5cde9cf36463959e"));
        assertThat(md5.getQueuedMessagesAndReplies(), is(0));
        assertThat(md5.getProcessedMessages(), is(1002L)); //1 string 1000 byte 1 ask
        assertThat(md5.getProcessedReplies(), is(1L)); // processed the ask reply for external call
        System.out.println(system);
        system.shutDown();
    }

    @Test
    public void testAskExternalFutureTypeFail() throws InterruptedException, ExecutionException {
        LintStoneSystem system = LintStoneSystemFactory.create();
        LintStoneActorAccessor md5 = system.registerActor("md5", Md5Actor::new, ActorSettings.DEFAULT);

        md5.send("This is my test string");
        for (int i = 0; i < 1000; i++) {
            md5.send(ByteBuffer.wrap(new byte[i]));
        }

        // wrong type cast, but pit it in Object, to avoid ClassCastException
        Object result = md5.<Integer>ask(new EndMessage()).get();

        // was no integer
        assertThat(result, is(instanceOf(String.class)));
    }

    @Test
    public void testAskExternal() throws InterruptedException {
        LintStoneSystem system = LintStoneSystemFactory.create();
        LintStoneActorAccessor md5 = system.registerActor("md5", Md5Actor::new, ActorSettings.DEFAULT);

        AtomicReference<String> result = new AtomicReference<>();
        md5.send("This is my test string");
        for (int i = 0; i < 1000; i++) {
            md5.send(ByteBuffer.wrap(new byte[i]));
        }
        md5.ask(new EndMessage(), mec -> mec.inCase(String.class, (x, m) -> {
            result.set(String.valueOf(x));
            latch.countDown();
        }));
        // wait for the result
        latch.await();

        // this should be repeatable correct. because the messages are processed in correct order and the ask will be the last one
        assertThat(result.get(), is("993e7b2144d8c8a5cde9cf36463959e"));
        assertThat(md5.getQueuedMessagesAndReplies(), is(0));
        assertThat(md5.getProcessedMessages(), is(1002L)); //1 string 1000 byte 1 ask
        assertThat(md5.getProcessedReplies(), is(1L)); // processed the ask reply for external call
        System.out.println(system);
        system.shutDown();
    }

}
