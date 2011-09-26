package ch.usi.dag.disl.test.stack;

import ch.usi.dag.disl.dislclass.annotation.AfterReturning;
import ch.usi.dag.disl.dislclass.snippet.marker.BytecodeMarker;
import ch.usi.dag.disl.dynamicinfo.DynamicContext;

public class DiSLClass {
	@AfterReturning(marker = BytecodeMarker.class, param="new", scope = "TargetClass.*")
	public static void precondition(DynamicContext dc) {
		dc.getStackValue(0, Object.class);
	}
}
