package ch.usi.dag.disl.test.suite.dispatch.junit;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import ch.usi.dag.disl.test.utils.ClientServerEvaluationRunner;


@RunWith (JUnit4.class)
public class DispatchTest {

    ClientServerEvaluationRunner r = new ClientServerEvaluationRunner (
        this.getClass ());


    @Test
    public void test ()
    throws Exception {
        r.start ();
        r.waitFor ();
        r.assertIsFinished ();
        if (Boolean.parseBoolean (System.getProperty ("disl.test.verbose"))) {
            r.destroyIfRunningAndFlushOutputs ();
        }
        r.assertIsSuccessfull ();
        r.assertEvaluationOut ("evaluation.out.resource");
        r.assertRestOutErrNull ();
    }


    @After
    public void cleanup () {
        r.destroy ();
    }
}