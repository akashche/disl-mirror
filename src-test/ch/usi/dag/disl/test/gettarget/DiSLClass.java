package ch.usi.dag.disl.test.gettarget;

import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.marker.BytecodeMarker;
import ch.usi.dag.disl.processorcontext.ArgumentProcessorContext;
import ch.usi.dag.disl.processorcontext.ArgumentProcessorMode;
import ch.usi.dag.disl.dynamiccontext.DynamicContext;

public class DiSLClass {

	@Before(marker = BytecodeMarker.class, args = "invokestatic", scope = "*.foo")
	public static void getTarget(ArgumentProcessorContext apc) {
		Object target = apc.getReceiver(ArgumentProcessorMode.CALLSITE_ARGS);
		System.out.println(target);
	}

	@Before(marker = BytecodeMarker.class, args = "invokevirtual", scope = "*.foo")
	public static void getTarget(GetTargetAnalysis gta, DynamicContext dc) {
		Object target = dc.getStackValue(gta.calleeParCount(), Object.class);
		System.out.println(target);
	}
}
