package ch.usi.dag.disl.test.args;

import ch.usi.dag.disl.annotation.AfterReturning;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.annotation.SyntheticLocal;
import ch.usi.dag.disl.marker.BodyMarker;
import ch.usi.dag.disl.processor.Processor;
import ch.usi.dag.disl.processor.ProcessorMode;
import ch.usi.dag.disl.staticcontext.StaticContext;

// This example shows how to emulate AspectJ's thisJoinPoint.getArgs()

public class DiSLClass {

	@SyntheticLocal
	public static Object[] args;
	
	@Before(marker = BodyMarker.class, scope = "*.*", guard = HasArgsGuard.class)
	public static void precondition(ArgsAnalysis ma) {
		args = new Object[ma.getNumberOfArgs()];
		// get the actual types of the arguments
		Processor.apply(ProcessorTest.class, ProcessorApplyType.INSIDE_METHOD);
	}

	@AfterReturning(marker = BodyMarker.class, scope = "*.*", guard = HasArgsGuard.class)
	public static void postcondition(StaticContext sc) {
		System.out.println("args for " + sc.thisMethodFullName() + " " + sc.thisMethodDescriptor());
	    for(int i = 0 ; i < args.length; i++) {
			System.out.println(" arg[" + i + "] " + args[i]);
		}
	}
}
