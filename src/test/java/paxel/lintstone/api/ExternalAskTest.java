package paxel.lintstone.api;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 *
 */
public class ExternalAskTest {

    CountDownLatch latch = new CountDownLatch(1);


    @Test
    public void testAskExternal() throws InterruptedException {
        LintStoneSystem system = LintStoneSystemFactory.createLimitedThreadCount(5);
        LintStoneActorAccess md5 = system.registerActor("md5", () -> new Md5Actor(), Optional.empty(), ActorSettings.create().setMulti(true).build());

        AtomicReference<String> result = new AtomicReference<>();
        md5.send("This is my test string");
        for (int i = 0; i < 1000; i++) {
            md5.send(ByteBuffer.wrap(new byte[i]));
        }
        md5.ask(new EndMessage(), mec -> {
            mec.inCase(String.class, (x, m) -> {
                result.set(String.valueOf(x));
                latch.countDown();
            });
        });
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

    private static class Md5Actor implements LintStoneActor {
        private MessageDigest md5;

        @Override
        public void newMessageEvent(LintStoneMessageEventContext mec) {
            mec.inCase(String.class, (name, m) -> {
                this.add(name.getBytes(StandardCharsets.UTF_8));
            }).inCase(ByteBuffer.class, (byteBuffer, m) -> {
                if (byteBuffer.hasArray())
                    add(byteBuffer.array());
            }).inCase(EndMessage.class, (dmg, m) -> {
                m.reply(getMd5String());
                // let's die
                m.unregister();
            });
        }

        private Object getMd5String() {
            byte[] digest = md5.digest();
            Formatter f = new Formatter(new StringBuilder());
            for (byte x :
                    digest) {
                f.format("%01x", x);
            }
            return f.toString();
        }

        private void add(byte[] bytes) {
            if (md5 == null) {
                try {
                    md5 = MessageDigest.getInstance("MD5");
                } catch (NoSuchAlgorithmException e) {
                    // ignorable for this test
                }
            }
            md5.update(bytes);
        }
    }
}
