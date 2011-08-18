package ch.usi.dag.disl.test.syntheticlocal;

import ch.usi.dag.disl.dislclass.annotation.AfterReturning;
import ch.usi.dag.disl.dislclass.annotation.Before;
import ch.usi.dag.disl.dislclass.annotation.SyntheticLocal;
import ch.usi.dag.disl.dislclass.annotation.SyntheticLocal.Initialize;
import ch.usi.dag.disl.dislclass.snippet.marker.BodyMarker;

public class DiSLClass {
	
	@SyntheticLocal(initialize=Initialize.ALWAYS)
	public static String flag = "1";
	
	@Before(marker = BodyMarker.class, scope = "TargetClass.print(boolean)", order = 0)
	public static void precondition() {
		System.out.println("Precondition!");
		
		// flag = "1";
		
		if(flag.equals("1")) {
			System.out.println("Precondition: This should be printed");
			return;
		}
		
		System.out.println("Precondition: This should NOT be printed");
	}
	
	@AfterReturning(marker = BodyMarker.class, scope = "TargetClass.print(boolean)", order = 0)
	public static void postcondition() {
		System.out.println("Postcondition!");
		
		if(flag.equals("1")) {
			System.out.println("Postcondition: This should be printed");
			return;
		}
		
		System.out.println("Postcondition: This should NOT be printed");
	}
}
