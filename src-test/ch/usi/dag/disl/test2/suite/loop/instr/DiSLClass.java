package ch.usi.dag.disl.test2.suite.loop.instr;

import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.marker.BasicBlockMarker;
import ch.usi.dag.disl.staticcontext.LoopStaticContext;

public class DiSLClass {

	@Before(marker = BasicBlockMarker.class, scope = "TargetClass.print()", order = 0)
	public static void precondition(LoopStaticContext lsc) {
		System.out.println("disl: enter basic block ! index: " + lsc.getBBindex()
				+ " loopstart? " + (lsc.isFirstOfLoop() ? "true" : "false"));
	}
}
