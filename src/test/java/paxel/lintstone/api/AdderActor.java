package paxel.lintstone.api;

/**
 *
 */
public class AdderActor implements LintStoneActor {

    private long sum;

    @Override
    public void newMessageEvent(LintStoneMessageEventContext mec) {
        mec.inCase(Integer.class, this::addInteger)
                .inCase(EndMessage.class, this::endSum)
                .otherwise((o, m) -> System.err.println("Unknown message " + o));
    }

    private void addInteger(Integer num, LintStoneMessageEventContext mec) {
        sum += num;
    }

    private void endSum(EndMessage end, LintStoneMessageEventContext mec) {
        mec.getActor("sumActor").send(sum);
    }
}
