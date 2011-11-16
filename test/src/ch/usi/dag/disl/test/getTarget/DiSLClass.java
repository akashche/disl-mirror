package ch.usi.dag.disl.test.getTarget;

import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.marker.BytecodeMarker;
import ch.usi.dag.disl.dynamiccontext.DynamicContext;

public class DiSLClass {

	@Before(marker = BytecodeMarker.class, args = "invokestatic", scope = "*.foo")
	public static void getTarget() {
		Object target = null;
		System.out.println(target);
	}

	@Before(marker = BytecodeMarker.class, args = "invokevirtual", scope = "*.foo")
	public static void getTarget(GetTargetAnalysis gta, DynamicContext dc) {
		Object target = dc.stackValue(gta.calleeParCount(), Object.class);
		System.out.println(target);
	}
}
