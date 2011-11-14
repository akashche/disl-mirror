package ch.usi.dag.disl.test.afterinit;

import ch.usi.dag.disl.dislclass.annotation.After;
import ch.usi.dag.disl.dislclass.annotation.Before;
import ch.usi.dag.disl.dislclass.snippet.marker.AfterInitBodyMarker;

public class DiSLClass {
	
	@Before(marker = AfterInitBodyMarker.class, scope = "*TargetClass2.<init>")
	public static void after() {
		System.out.println("Before");
	}
	
	@After(marker = AfterInitBodyMarker.class, scope = "*TargetClass2.<init>")
	public static void afterThrowning() {
		System.out.println("After");
	}
}
