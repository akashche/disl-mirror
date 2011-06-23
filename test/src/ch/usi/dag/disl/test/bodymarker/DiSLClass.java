package ch.usi.dag.disl.test.bodymarker;

import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.annotation.SyntheticLocal;
import ch.usi.dag.disl.snippet.marker.BodyMarker;

public class DiSLClass {
	
	@SyntheticLocal
	static String one = "2";
	
	@SyntheticLocal
	static int intType = 2;
	
	@Before(marker = BodyMarker.class, scope = "TargetClass.print(boolean)", order = 0)
	public static void precondition() {
		
		System.out.println("Precondition!");
		
		// TODO ! enable for testing
		//System.out.println("Synthetic local one=" + one + ", intType=" + intType);
		//one = "1";
		//intType = 1;
		final String otherOne = "1";
		
		if(one.equals(otherOne)) {
			System.out.println("Precondition: This should be printed");
			return;
		}
		
		System.out.println("Precondition: This should NOT be printed");
	}
	
	@After(marker = BodyMarker.class, scope = "TargetClass.print(boolean)", order = 0)
	public static void postcondition() {
		
		System.out.println("Postcondition!");
		
		// TODO ! enable for testing
		//System.out.println("Synthetic local one=" + one + ", intType=" + intType);
	}
}
