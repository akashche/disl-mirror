package ch.usi.dag.disl.test.processor;

import ch.usi.dag.disl.dislclass.annotation.Before;
import ch.usi.dag.disl.dislclass.annotation.SyntheticLocal;
import ch.usi.dag.disl.dislclass.snippet.marker.BodyMarker;
import ch.usi.dag.disl.dislclass.snippet.marker.BytecodeMarker;
import ch.usi.dag.disl.processor.Processor;
import ch.usi.dag.disl.processor.ProcessorApplyType;
import ch.usi.dag.disl.staticinfo.analysis.StaticContext;

public class DiSLClass {

	@SyntheticLocal
	public static String flag = "Start";

	@Before(marker = BodyMarker.class, order = 0, scope = "TargetClass.method*")
	public static void insideMethod(StaticContext ci) {
		
		System.out.println("(In) Method " + ci.thisMethodName() + ": ");
		System.out.println(flag);
		
		Processor.apply(ProcessorTest.class, ProcessorApplyType.INSIDE_METHOD);
		
		System.out.println(flag);
		System.out.println(ProcessorTest.flag);
	}
	
	@Before(marker = BytecodeMarker.class, param="invokevirtual", order = 0, scope = "TargetClass.main")
	public static void beforeInvocation(StaticContext ci) {
		
		System.out.println("(Before) Method " + ci.thisMethodName() + ": ");
		
		Processor.apply(ProcessorTest.class, ProcessorApplyType.BEFORE_INVOCATION);
		
		System.out.println(ProcessorTest.flag);
	}
}
