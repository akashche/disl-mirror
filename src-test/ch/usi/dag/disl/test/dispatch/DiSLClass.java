package ch.usi.dag.disl.test.dispatch;

import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.marker.BodyMarker;
import ch.usi.dag.dislre.REDispatch;

public class DiSLClass {
	
	@After(marker = BodyMarker.class, scope = "TargetClass.empty", order=0)
	public static void emptypostcondition() {
		REDispatch.analyse(1, 1);
	}
}
