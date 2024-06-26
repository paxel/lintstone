package paxel.lintstone.api;

import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import paxel.lintstone.api.actors.StupidActor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

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
    List<Object> errorMessage = new ArrayList<>();

    public FailingTests() {
    }

    private ErrorHandlerDecision addError(Object o) {
        errorMessage.add(o);
        return ErrorHandlerDecision.CONTINUE;
    }

    @BeforeEach
    void init() {
        errorMessage.clear();
    }

    @Test
    public void testFailedMessageResponse() throws InterruptedException {
        LintStoneSystem system = LintStoneSystemFactory.create();
        system.registerActor(STOP_ACTOR, () -> a -> {
            System.out.println("stop received. countdown latch");
            latch.countDown();
        }, ActorSettings.create().setErrorHandler(this::addError).build());


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
            Assert.fail("Timed out without activating latch");

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

    @Test
    public void testGetDataOut() throws InterruptedException, ExecutionException {
        LintStoneSystem system = LintStoneSystemFactory.create();


        LintStoneActorAccessor echoActor = system.registerActor(ECHO_ACTOR, () -> a -> a.reply("echo"), ActorSettings.create().setErrorHandler(this::addError).build());

        // this message goes to an actor that wants to reply. but can't, because we are calling from outside the actor system
        // so this should be a message in the errorHandler
        echoActor.tell("the error log is expected.");
        // this is the correct way to ask for data from outside the actorSystem
        String echo = echoActor.<String>ask("please tell me").get();
        assertThat(echo, is("echo"));

        // the first try should have created a message here,
        // but currently it's just a log message
        assertThat(errorMessage.size(), is(0));

        system.shutDownAndWait();
    }


}
