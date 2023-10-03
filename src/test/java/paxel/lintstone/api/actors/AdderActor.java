package paxel.lintstone.api.actors;

import paxel.lintstone.api.LintStoneActor;
import paxel.lintstone.api.LintStoneMessageEventContext;
import paxel.lintstone.api.messages.DieMessage;
import paxel.lintstone.api.messages.EndMessage;

public class AdderActor implements LintStoneActor {

    private long sum;
    private String name;


    @Override
    public void newMessageEvent(LintStoneMessageEventContext mec) {
        mec.inCase(Integer.class, this::addInteger)
                .inCase(EndMessage.class, this::endSum)
                .inCase(String.class, this::name)
                .inCase(DieMessage.class, this::unregister)
                .otherwise(this::other);
    }

    private void addInteger(Integer num, LintStoneMessageEventContext mec) {
        sum += num;
    }

    private void endSum(EndMessage end, LintStoneMessageEventContext mec) {
        mec.getActor("sumActor").tell(sum);
    }

    private void name(String msg, LintStoneMessageEventContext mec) {
        this.name = msg;
    }

    private void unregister(DieMessage msg, LintStoneMessageEventContext mec) {
        boolean unregister = mec.unregister();
        System.out.println("Actor " + name + " unregistered: " + unregister);
    }

    private void other(Object o, LintStoneMessageEventContext m) {
        System.err.println("Unknown message " + o);
    }

}
