package ch.usi.dag.disl.test.exception;

import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.marker.BodyMarker;
import ch.usi.dag.disl.marker.BytecodeMarker;

public class DiSLClass {

	@Before(marker = BytecodeMarker.class, args = "invokevirtual", scope = "TargetClass.bar")
	public static void precondition() {
		try {
			Integer.valueOf("1.0");
		} catch (Exception e) {
			System.out.println("exception handler from snippet");
		}
	}

	@After(marker = BodyMarker.class, scope = "TargetClass.<init>")
	public static void after() {
		try {
			System.out.println("exception body from snippet");
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
}
