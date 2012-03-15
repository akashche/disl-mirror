package ch.usi.dag.disl.example.sharing.instrument;

import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.classcontext.ClassContext;
import ch.usi.dag.disl.dynamiccontext.DynamicContext;
import ch.usi.dag.disl.example.sharing.runtime.SharedFieldsAnalysis;
import ch.usi.dag.disl.marker.BytecodeMarker;

public class SharedFieldsInstrumentation {

	@Before(marker=BytecodeMarker.class, args = "getfield", dynamicBypass = true)
	public static void beforeFieldRead(FieldAccessStaticContext sc, DynamicContext dc, ClassContext cc) {
		SharedFieldsAnalysis.getInstance().onFieldRead(dc.getStackValue(0, Object.class), cc.asClass(sc.getOwner()), sc.getFieldId());
	}

	@Before(marker = BytecodeMarker.class, args = "putfield", dynamicBypass = true)
	public static void beforeFieldWrite(FieldAccessStaticContext sc, DynamicContext dc, ClassContext cc) {
		SharedFieldsAnalysis.getInstance().onFieldWrite(dc.getStackValue(1, Object.class), cc.asClass(sc.getOwner()), sc.getFieldId());
	}
	
	@Before(marker = BytecodeMarker.class, args = "arraylength", dynamicBypass = true)
	public static void beforeArrayLength(FieldAccessStaticContext sc, DynamicContext dc) {
		SharedFieldsAnalysis.getInstance().onArrayLength(dc.getStackValue(0, Object.class));
	}
	
	@Before(marker = BytecodeMarker.class, args = "aaload,baload,caload,daload,faload,iaload,laload,saload", dynamicBypass = true)
	public static void beforeArrayRead(FieldAccessStaticContext sc, DynamicContext dc) {
		SharedFieldsAnalysis.getInstance().onArrayRead(dc.getStackValue(1, Object.class));
	}
	
	@Before(marker = BytecodeMarker.class, args = "aastore,bastore,castore,dastore,fastore,iastore,lastore,sastore", dynamicBypass = true)
	public static void beforeArrayWrite(FieldAccessStaticContext sc, DynamicContext dc) {
		SharedFieldsAnalysis.getInstance().onArrayWrite(dc.getStackValue(2, Object.class));
	}
}
