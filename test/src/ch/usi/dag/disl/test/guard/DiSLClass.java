package ch.usi.dag.disl.test.guard;

import ch.usi.dag.disl.dislclass.annotation.After;
import ch.usi.dag.disl.dislclass.annotation.Before;
import ch.usi.dag.disl.dislclass.snippet.marker.BodyMarker;
import ch.usi.dag.disl.dislclass.snippet.marker.BytecodeMarker;
import ch.usi.dag.disl.processor.Processor;
import ch.usi.dag.disl.processor.ProcessorApplyType;
import ch.usi.dag.disl.staticinfo.analysis.StaticContext;

public class DiSLClass {

	@Before(marker = BodyMarker.class, order = 0, scope = "TargetClass.m*", guard=GuardYes.class)
	public static void beforeInvocation(StaticContext ci) {
		
		System.out.println("This should be printed - before");
		
		Processor.apply(ProcessorTest.class, ProcessorApplyType.INSIDE_METHOD);
	}
	
	@Before(marker = BytecodeMarker.class, param="invokevirtual", order = 1, scope = "TargetClass.main", guard=GuardNo.class)
	public static void guarded(StaticContext ci) {
		
		System.out.println("This should not be printed");
	}
	
	@After(marker = BodyMarker.class, scope = "TargetClass.m*")
	public static void afterInvocation(StaticContext ci) {
		
		System.out.println("This should be printed - after");
		
		Processor.apply(ProcessorTest2.class, ProcessorApplyType.INSIDE_METHOD);
	}
}
