package ch.usi.dag.disl.test2.suite.dispatch.junit;

import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import ch.usi.dag.disl.test2.utils.*;

@RunWith(JUnit4.class)
public class DispatchTest {

	ClientServerRunner r = new ClientServerRunner(this.getClass());
	
	@Test
	public void test() 
			throws Exception {
		// FIXME
		fail("FIXME");
	}	
	
	@After
	public void cleanup() {
		r.destroy();
	}
}

