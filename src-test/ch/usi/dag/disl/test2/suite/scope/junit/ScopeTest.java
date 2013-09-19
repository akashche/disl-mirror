package ch.usi.dag.disl.test2.suite.scope.junit;

import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import ch.usi.dag.disl.test2.utils.*;

@RunWith(JUnit4.class)
public class ScopeTest {

	ClientServerRunner r = new ClientServerRunner(this.getClass());
	
	@Test
	public void test() 
			throws Exception {	
		// FIXME
		fail("FIXME");
		// jvm crash
		
		/*r.start();		
		r.waitFor();
		r.assertIsFinished();
		if(Boolean.parseBoolean(System.getProperty("disl.test.verbose"))) { r.destroyIfRunningAndFlushOutputs(); }
		r.assertIsSuccessfull();
		r.assertClientOut("client.out.resource");
		r.assertClientOutNull();
		r.assertClientErrNull();
		r.assertServerOutNull();
		r.assertServerErrNull();*/
	}	
	
	@After
	public void cleanup() {
		r.destroy();
	}
}

