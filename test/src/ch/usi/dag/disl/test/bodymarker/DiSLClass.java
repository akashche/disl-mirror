package ch.usi.dag.disl.test.bodymarker;

import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.snippet.marker.BodyMarker;

public class DiSLClass {
	
	@Before(marker = BodyMarker.class, scope = "TargetClass.print()", order = 0)
	public static void precondition() {
		
		final String one = "1";
		final String otherOne = "1";
		
		System.out.println("Precondition!");
		
		if(one.equals(otherOne)) {
			System.out.println("Precondition: This should be printed");
			return;
		}
		
		System.out.println("Precondition: This should NOT be printed");
	}
	
	@After(marker = BodyMarker.class, scope = "TargetClass.print()", order = 0)
	public static void postcondition() {
		System.out.println("Postcondition!");
	}
}
