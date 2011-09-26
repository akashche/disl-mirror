package ch.usi.dag.disl.test.getTarget;

import ch.usi.dag.disl.dislclass.annotation.Before;
import ch.usi.dag.disl.dislclass.snippet.marker.BytecodeMarker;
import ch.usi.dag.disl.dynamicinfo.DynamicContext;

public class DiSLClass {

	@Before(marker = BytecodeMarker.class, param = "invokestatic", scope = "*.foo")
	public static void getTarget() {
		Object target = null;
		System.out.println(target);
	}

	@Before(marker = BytecodeMarker.class, param = "invokevirtual", scope = "*.foo")
	public static void getTarget(GetTargetAnalysis gta, DynamicContext dc) {
		Object target = dc.getStackValue(gta.calleeParCount(), Object.class);
		System.out.println(target);
	}
}
