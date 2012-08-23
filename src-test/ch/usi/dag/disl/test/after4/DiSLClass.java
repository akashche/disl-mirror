package ch.usi.dag.disl.test.after4;

import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.annotation.AfterThrowing;
import ch.usi.dag.disl.dynamiccontext.DynamicContext;
import ch.usi.dag.disl.marker.BodyMarker;

public class DiSLClass {

	@After(marker = BodyMarker.class, scope = "TargetClass.print(boolean)", order = 0)
	public static void after(DynamicContext dc) {
		System.out.println("after " + dc.getException());
	}

	@AfterThrowing(marker = BodyMarker.class, scope = "TargetClass.print(boolean)", order = 1)
	public static void afterThrowning(DynamicContext dc) {
		System.out.println("afterThrowning " + dc.getException());
	}
}
