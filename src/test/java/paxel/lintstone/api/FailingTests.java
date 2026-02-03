package paxel.lintstone.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import paxel.lintstone.api.actors.StupidActor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 */
public class FailingTests {

    public static final String ECHO_ACTOR = "echo-actor";
    public static final String GATEWAY = "gateway";
    public static final String STOP_ACTOR = "end-processor";
    public static final String FAILING = "failing";
    public static final String NOT_EXISTENT = "no";
    final CountDownLatch latch = new CountDownLatch(1);
    private final List<Object> errorMessage = Collections.synchronizedList(new ArrayList<>());

    public FailingTests() {
    }

    private ErrorHandlerDecision addError(LintStoneError error, String description, Throwable cause) {
        errorMessage.add(cause);
        return ErrorHandlerDecision.CONTINUE;
    }

    @BeforeEach
    public void init() {
        errorMessage.clear();
    }

    @Test
    public void testFailedMessageResponse() throws InterruptedException {
        LintStoneSystem system = LintStoneSystemFactory.create();
        system.registerActor(STOP_ACTOR, () -> a -> a.otherwise((msg, ctx) -> {
            System.out.println("stop received. countdown latch");
            latch.countDown();
        }), ActorSettings.create().setErrorHandler(this::addError).build());


        // This creates an actor that will create a FAILING actor
        LintStoneActorAccessor stupid = system.registerActor(GATEWAY, StupidActor::new, ActorSettings.create().setErrorHandler(this::addError).build());

        // this will create the error handler
        // send a "Hi" to the error handler
        // that fails
        // the failure causes a FailedMessage in the GATEWAY
        // that will cause the stop
        stupid.tell("Don't be stupid");


        boolean await = latch.await(10, TimeUnit.SECONDS);
        if (!await)
            Assertions.fail("Timed out without activating latch");

        System.out.println("Test is finished");
        // wait for the result
        System.out.println(system);
        system.shutDownAndWait();
        System.out.println("wait");
        system.shutDownNow();
        System.out.println("now");
        system.shutDown();
        System.out.println("down");
    }

    private void waitForErrorSize(int expectedSize) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (errorMessage.size() < expectedSize && System.currentTimeMillis() - start < 5000) {
            Thread.sleep(10);
        }
    }

    @Test
    public void testGetDataOut() throws InterruptedException, ExecutionException {
        LintStoneSystem system = LintStoneSystemFactory.create();


        LintStoneActorAccessor echoActor = system.registerActor(ECHO_ACTOR, () -> a -> a.otherwise((msg, ctx) -> ctx.reply("echo")), ActorSettings.create().setErrorHandler(this::addError).build());

        // this message goes to an actor that wants to reply. but can't, because we are calling from outside the actor system
        // so this should be a message in the errorHandler
        echoActor.tell("the error log is expected.");
        // this is the correct way to ask for data from outside the actorSystem
        String echo = echoActor.<String>ask("please tell me").get();
        assertThat(echo).isEqualTo("echo");

        // the first try should have created a message here,
        // and now it does because we fixed the error propagation
        waitForErrorSize(1);
        assertThat(errorMessage).hasSize(1);

        system.shutDownAndWait();
    }


}
