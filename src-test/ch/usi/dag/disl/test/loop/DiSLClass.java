package ch.usi.dag.disl.test.loop;

import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.marker.BasicBlockMarker;
import ch.usi.dag.disl.staticcontext.LoopStaticContext;

public class DiSLClass {

	@Before(marker = BasicBlockMarker.class, scope = "TargetClass.print()", order = 2)
	public static void precondition(LoopStaticContext lsc) {
		System.out.println("Enter basic block ! index: " + lsc.getBBindex()
				+ " loopstart? " + (lsc.isFirstOfLoop() ? "true" : "false"));
	}
}
