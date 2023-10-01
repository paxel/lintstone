package paxel.lintstone.api.actors;

import paxel.lintstone.api.LintStoneActor;
import paxel.lintstone.api.LintStoneMessageEventContext;
import paxel.lintstone.api.messages.DieMessage;

import java.util.function.Consumer;

public class SumActor implements LintStoneActor {

    private final Consumer<Long> result;
    private String name;

    public SumActor(Consumer<Long> result) {
        this.result = result;
    }
    private long sum;
    private int expected;
    private int received;

    private static void otherwise(Object o, LintStoneMessageEventContext m) {
        System.err.println("Unknown message " + o);
    }

    @Override
    public void newMessageEvent(LintStoneMessageEventContext mec) {
        mec
                .inCase(Long.class, this::addLong)
                .inCase(String.class, this::incExpected)
                .otherwise(SumActor::otherwise);
    }

    private void addLong(Long num, LintStoneMessageEventContext mec) {
        sum += num;
        received++;
        if (received == expected) {
            result.accept(sum);
        }
        // Tell the actor, that he can unregister
        mec.reply(new DieMessage());
    }

    private void incExpected(String name, LintStoneMessageEventContext mec) {
        expected++;
        System.out.println("Actor " + name + " registered");
    }

}
