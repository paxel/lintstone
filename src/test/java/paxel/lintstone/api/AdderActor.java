package paxel.lintstone.api;

public class AdderActor implements LintStoneActor {

    private long sum;
    private String name;
    private final int last = -1;

    @Override
    public void newMessageEvent(LintStoneMessageEventContext mec) {
        mec.inCase(Integer.class, this::addInteger)
                .inCase(EndMessage.class, this::endSum)
                .inCase(String.class, this::name)
                .inCase(DieMessage.class, this::unregister)
                .otherwise((o, m) -> System.err.println("Unknown message " + o));
    }

    private void addInteger(Integer num, LintStoneMessageEventContext mec) {
        if (last >= num) {

            // make sure that the order is correct
            throw new IllegalStateException("Expected something bigger than " + last + " but got "+num);
        }
        sum += num;
    }

    private void endSum(EndMessage end, LintStoneMessageEventContext mec) {
        mec.getActor("sumActor").send(sum);
    }

    private void name(String msg, LintStoneMessageEventContext mec) {
        this.name = msg;
    }

    private void unregister(DieMessage msg, LintStoneMessageEventContext mec) {
        boolean unregister = mec.unregister();
        System.out.println("Actor " + name + " unregistered: " + unregister);
    }

}
