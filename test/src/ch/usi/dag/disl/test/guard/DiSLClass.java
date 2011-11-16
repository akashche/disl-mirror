package ch.usi.dag.disl.test.guard;

import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.marker.BodyMarker;
import ch.usi.dag.disl.marker.BytecodeMarker;
import ch.usi.dag.disl.processor.Processor;
import ch.usi.dag.disl.processor.ProcessorMode;
import ch.usi.dag.disl.staticcontext.MethodSC;

public class DiSLClass {

	@Before(marker = BodyMarker.class, order = 0, scope = "TargetClass.m*", guard=GuardYes.class)
	public static void beforeInvocation(MethodSC ci) {
		
		System.out.println("This should be printed - before");
		
		Processor.apply(ProcessorTest.class, ProcessorMode.METHOD_ARGS);
	}
	
	@Before(marker = BytecodeMarker.class, args="invokevirtual", order = 1, scope = "TargetClass.main", guard=GuardNo.class)
	public static void guarded(MethodSC ci) {
		
		System.out.println("This should not be printed");
	}
	
	@After(marker = BodyMarker.class, scope = "TargetClass.m*")
	public static void afterInvocation(MethodSC ci) {
		
		System.out.println("This should be printed - after");
		
		Processor.apply(ProcessorTest2.class, ProcessorMode.METHOD_ARGS);
	}
}
