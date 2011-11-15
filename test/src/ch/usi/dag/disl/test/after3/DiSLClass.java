package ch.usi.dag.disl.test.after3;

import ch.usi.dag.disl.annotation.AfterReturning;
import ch.usi.dag.disl.annotation.AfterThrowing;
import ch.usi.dag.disl.marker.BodyMarker;
import ch.usi.dag.disl.staticcontext.StaticContext;

public class DiSLClass {
	
	@AfterThrowing(marker = BodyMarker.class, scope = "TargetClass.e*", order = 0)
	public static void throwing(StaticContext sc) {
		System.out.println("Throwing! for " + sc.thisMethodName());
	}
	
	@AfterReturning(marker = BodyMarker.class, scope = "TargetClass.e*", order = 0)
	public static void returning(StaticContext sc) {
		System.out.println("Returning! for " + sc.thisMethodName());
	}
}
