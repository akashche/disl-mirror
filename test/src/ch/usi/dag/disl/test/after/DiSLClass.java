package ch.usi.dag.disl.test.after;

import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.annotation.AfterThrowing;
import ch.usi.dag.disl.snippet.marker.BodyMarker;

public class DiSLClass {
	
	@After(marker = BodyMarker.class, scope = "TargetClass.print(boolean)", order = 0)
	public static void after1() {
		System.out.println("after1!");
	}
	
	@AfterThrowing(marker = BodyMarker.class, scope = "TargetClass.print(boolean)", order = 1)
	public static void afterThrowning1() {
		System.out.println("afterThrowning1!");
	}
}
