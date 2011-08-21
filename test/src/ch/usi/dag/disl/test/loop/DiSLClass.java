package ch.usi.dag.disl.test.loop;

import ch.usi.dag.disl.dislclass.annotation.Before;
import ch.usi.dag.disl.dislclass.snippet.marker.BasicBlockMarker;
import ch.usi.dag.disl.staticinfo.analysis.BasicBlockAnalysis;

public class DiSLClass {

	@Before(marker = BasicBlockMarker.class, scope = "TargetClass.print()", order = 2)
	public static void precondition(BasicBlockAnalysis bba) {
		System.out.println("Enter basic block ! index: " + bba.getBBindex()
				+ " loopstart? " + (bba.isFirstOfLoop() ? "true" : "false"));
	}
}
