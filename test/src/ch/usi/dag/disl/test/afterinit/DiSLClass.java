package ch.usi.dag.disl.test.afterinit;

import ch.usi.dag.disl.dislclass.annotation.After;
import ch.usi.dag.disl.dislclass.annotation.Before;
import ch.usi.dag.disl.dislclass.snippet.marker.BodyMarker;

public class DiSLClass {
	
	@Before(marker = BodyMarker.class, scope = "*TargetClass2.<init>")
	public static void after() {
		System.out.println("Before");
	}
	
	@After(marker = BodyMarker.class, scope = "*TargetClass2.<init>")
	public static void afterThrowning() {
		System.out.println("After");
	}
}
