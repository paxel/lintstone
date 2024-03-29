package paxel.lintstone.api;

import org.junit.Test;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.*;

/**
 *
 */
public class ReplyTest {

    private static final Random R = new Random(0xbadbee);
    final CountDownLatch latch = new CountDownLatch(1);

    public ReplyTest() {
    }

    @Test
    public void testSomeMethod() throws InterruptedException {
        LintStoneSystem system = LintStoneSystemFactory.create();
        LintStoneActorAccessor alex = system.registerActor("Alex", () -> new FightActor(50, 1, 12, 0, 0),
                ActorSettings.DEFAULT, "Uta");
        LintStoneActorAccessor uta = system.registerActor("Uta", () -> new FightActor(40, 3, 4, 1, 1),
                ActorSettings.DEFAULT, "Alex");
        LintStoneActorAccessor floor = system.registerActor("floor", () -> a -> {
            // someone died
            a.inCase(String.class, (n, mec) -> System.out.println(n + " lost"));
            latch.countDown();
        }, ActorSettings.DEFAULT);

        uta.tell(new StartMessage());

        // wait for the result
        latch.await();
        assertThat(system.unregisterActor("Alex"), is(true));
        assertThat(system.unregisterActor("Uta"), is(true));
        assertThat(system.unregisterActor("floor"), is(true));
        //is it really removed?
        assertThat(system.unregisterActor("floor"), is(false));
        system.shutDown();
    }

    private static class FightActor implements LintStoneActor {

        private int hp;
        private final int attacks;
        private final int dice;
        private final int plus;
        private int healthPotions;
        private LintStoneActorAccessor nme;

        public FightActor(int hp, int attacks, int dice, int plus, int healthPotions) {
            this.hp = hp;
            this.attacks = attacks;
            this.dice = dice;
            this.plus = plus;
            this.healthPotions = healthPotions;
        }

        @Override
        public void newMessageEvent(LintStoneMessageEventContext mec) {
            mec.inCase(String.class, (name, m) -> {
                this.nme = m.getActor(name);
                System.out.println(m.getName() + " challenges " + name);
            }).inCase(StartMessage.class, (ignore, m) -> attack(m)).inCase(Integer.class, (dmg, m) -> {
                hp = hp - dmg;
                if (hp < 0) {
                    System.out.println(m.getName() + " goes down");
                    m.tell("floor", m.getName());
                } else if (hp < 15 && healthPotions > 0) {
                    heal(m);
                } else {
                    System.out.println(m.getName() + " is at " + hp + " hp");
                    attack(m);
                }
            });
        }

        private void attack(LintStoneMessageEventContext m) throws UnregisteredRecipientException {
            final Integer calcDmg = calcDmg();
            System.out.println(m.getName() + " attacks for " + calcDmg);
            nme.tell(calcDmg);
        }

        private Integer calcDmg() {
            int result = 0;
            for (int i = 0; i < attacks; i++) {
                result += R.nextInt(dice) + plus;
            }
            return result;
        }

        private void heal(LintStoneMessageEventContext m) {
            final int heal = R.nextInt(25);
            System.out.println(m.getName() + " heals for " + heal);
            hp += heal;
            healthPotions--;
            // did not attack: no damage
            nme.tell(0);
        }

    }

    private static class StartMessage {

        public StartMessage() {
        }
    }

}
