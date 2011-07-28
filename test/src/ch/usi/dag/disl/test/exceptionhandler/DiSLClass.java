package ch.usi.dag.disl.test.exceptionhandler;

import ch.usi.dag.disl.annotation.AfterReturning;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.snippet.marker.ExceptionHandlerMarker;

public class DiSLClass {
	
	@Before(marker = ExceptionHandlerMarker.class, scope = "TargetClass.print(boolean)", order = 0)
	public static void precondition() {
		System.out.println("@Before");
	}
	
	@AfterReturning(marker = ExceptionHandlerMarker.class, scope = "TargetClass.print(boolean)", order = 0)
	public static void postcondition() {
		System.out.println("@AfterReturning");
	}
}
