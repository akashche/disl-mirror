package ch.usi.dag.disl.test2.suite.loop.junit;

import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import ch.usi.dag.disl.test2.utils.*;

@RunWith(JUnit4.class)
public class LoopTest {

	ClientServerRunner r = new ClientServerRunner(this.getClass());
	
	@Test
	public void test() 
			throws Exception {
		fail("FIXME");
		// FIXME
		// see client.out.1.resource and client.out.2.resource
		// test might nondeterministically result in both of them
		
		r.start();			
		r.waitFor();
		r.assertIsFinished();
		if(Boolean.parseBoolean(System.getProperty("disl.test.verbose"))) { r.destroyIfRunningAndFlushOutputs(); }
		r.assertIsSuccessfull();
		r.assertClientOut("client.out.resource");
		r.assertClientErrNull();
		r.assertServerOutNull();
		r.assertServerErrNull();
	}	
	
	@After
	public void cleanup() {
		r.destroy();
	}
}

