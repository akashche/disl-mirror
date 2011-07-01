package ch.usi.dag.disl.test.bbmarker;

import ch.usi.dag.disl.annotation.AfterReturning;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.snippet.marker.BasicBlockMarker;

public class DiSLClass {
	
	@Before(marker = BasicBlockMarker.class, scope = "TargetClass.print(boolean)", order = 0)
	public static void precondition() {
		System.out.println("Enter basic block!");
	}
	
	@AfterReturning(marker = BasicBlockMarker.class, scope = "TargetClass.print(boolean)", order = 0)
	public static void postcondition() {
		System.out.println("Exit basic block!");
	}
}
