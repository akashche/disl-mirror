package ch.usi.dag.disl.test.catchclause;

import ch.usi.dag.disl.annotation.AfterReturning;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.snippet.marker.CatchClauseMarker;

public class DiSLClass {
	
	@Before(marker = CatchClauseMarker.class, scope = "TargetClass.print(boolean)", order = 0)
	public static void precondition() {
		System.out.println("@Before");
	}
	
	@AfterReturning(marker = CatchClauseMarker.class, scope = "TargetClass.print(boolean)", order = 0)
	public static void postcondition() {
		System.out.println("@AfterReturning");
	}
}
