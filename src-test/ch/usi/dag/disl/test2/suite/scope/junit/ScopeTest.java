package ch.usi.dag.disl.test2.suite.scope.junit;

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
		r.start();			
		// FIXME
		//r.waitFor();
		//r.assertIsNotRunning();
		//r.flushClientOut();
		//r.flushClientErr();
		//r.assertIsFinished();
		//r.assertClientOut("client.out.resource");
		//r.assertClientOutNull();
		//r.assertClientErrNull();
		//r.assertServerOutNull();
		//r.assertServerErrNull();

		if(Boolean.parseBoolean(System.getProperty("disl.test.verbose"))) {
			r.verbose();
		}
	}	
	
	@After
	public void cleanup() {
		r.destroy();
	}
}

