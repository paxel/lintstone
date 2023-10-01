package paxel.lintstone.api.actors;

import paxel.lintstone.api.LintStoneActor;
import paxel.lintstone.api.LintStoneMessageEventContext;
import paxel.lintstone.api.messages.EndMessage;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

public class Md5Actor implements LintStoneActor {
    private MessageDigest md5;

    @Override
    public void newMessageEvent(LintStoneMessageEventContext mec) {
        mec
                .inCase(String.class, this::handleString)
                .inCase(ByteBuffer.class, this::handleByteBuffer)
                .inCase(EndMessage.class, this::handleEnd);
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

    private void handleString(String name, LintStoneMessageEventContext m) {
        this.add(name.getBytes(StandardCharsets.UTF_8));
    }

    private void handleByteBuffer(ByteBuffer byteBuffer, LintStoneMessageEventContext m) {
        if (byteBuffer.hasArray())
            add(byteBuffer.array());
    }

    private void handleEnd(EndMessage dmg, LintStoneMessageEventContext m) {
        m.reply(getMd5String());
// let's die
        m.unregister();
    }
}
