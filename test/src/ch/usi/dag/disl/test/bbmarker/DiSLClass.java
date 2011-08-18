package ch.usi.dag.disl.test.bbmarker;

import ch.usi.dag.disl.dislclass.annotation.AfterReturning;
import ch.usi.dag.disl.dislclass.annotation.Before;
import ch.usi.dag.disl.dislclass.snippet.marker.BasicBlockMarker;
import ch.usi.dag.disl.dislclass.snippet.marker.PreciseBasicBlockMarker;
import ch.usi.dag.disl.staticinfo.analysis.BasicBlockAnalysis;

public class DiSLClass {

	@Before(marker = PreciseBasicBlockMarker.class, scope = "TargetClass.print(boolean)", order = 0)
	public static void precondition() {
		System.out.println("Enter basic block!");
	}

	@AfterReturning(marker = PreciseBasicBlockMarker.class, scope = "TargetClass.print(boolean)", order = 1)
	public static void postcondition() {
		System.out.println("Exit basic block!");
	}

	@Before(marker = BasicBlockMarker.class, scope = "TargetClass.print(boolean)", order = 2)
	public static void precondition1(BasicBlockAnalysis bba) {
		System.out.println("Enter basic block 1! index: " + bba.getBBindex()
				+ " size: " + bba.getBBSize());
	}

	@AfterReturning(marker = BasicBlockMarker.class, scope = "TargetClass.print(boolean)", order = 3)
	public static void postcondition1() {
		System.out.println("Exit basic block 1!");
	}
}
