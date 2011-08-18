package ch.usi.dag.disl.test.exception;

import ch.usi.dag.disl.dislclass.annotation.Before;
import ch.usi.dag.disl.dislclass.snippet.marker.BytecodeMarker;

public class DiSLClass {

	@Before(marker = BytecodeMarker.class, param = "invokevirtual", scope = "TargetClass.bar")
	public static void precondition() {
		try {
			Integer.valueOf("1.0");
		} catch (Exception e) {
			System.out.println("exception handler from snippet");
		}
	}

}
