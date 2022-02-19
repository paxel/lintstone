package paxel.lintstone.api;

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

    @Override
    public void newMessageEvent(LintStoneMessageEventContext mec) {
        mec.inCase(Long.class, this::addLong)
                .inCase(String.class, this::incExpected)
                .otherwise((o, m) -> System.err.println("Unknown message " + o));
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
