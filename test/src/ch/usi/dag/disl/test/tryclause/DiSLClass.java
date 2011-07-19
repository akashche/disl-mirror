package ch.usi.dag.disl.test.tryclause;

import ch.usi.dag.disl.annotation.AfterReturning;
import ch.usi.dag.disl.annotation.AfterThrowing;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.snippet.marker.TryClauseMarker;

public class DiSLClass {
	
	@Before(marker = TryClauseMarker.class, scope = "TargetClass.print(boolean)", order = 0)
	public static void precondition() {
		System.out.println("@Before");
	}
	
	@AfterReturning(marker = TryClauseMarker.class, scope = "TargetClass.print(boolean)", order = 0)
	public static void postcondition() {
		System.out.println("@AfterReturning");
	}
	
	@AfterThrowing(marker = TryClauseMarker.class, scope = "TargetClass.print(boolean)", order = 0)
	public static void postcondition1() {
		System.out.println("@AfterThrowing");
	}
}
