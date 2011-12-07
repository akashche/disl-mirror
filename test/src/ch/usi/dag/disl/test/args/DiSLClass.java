package ch.usi.dag.disl.test.args;

import ch.usi.dag.disl.annotation.AfterReturning;
import ch.usi.dag.disl.marker.BodyMarker;
import ch.usi.dag.disl.processorcontext.ProcessorContext;
import ch.usi.dag.disl.processorcontext.ProcessorMode;
import ch.usi.dag.disl.staticcontext.MethodStaticContext;

public class DiSLClass {

	@AfterReturning(marker = BodyMarker.class, scope = "TargetClass.*")
	public static void postcondition(MethodStaticContext sc, ProcessorContext pc) {
		
		System.out.println("args for " + sc.thisMethodFullName() + " " + sc.thisMethodDescriptor());
		
		Object[] args = pc.getArgs(ProcessorMode.METHOD_ARGS);
		for(int i = 0 ; i < args.length; ++i) {
			System.out.println(" arg[" + i + "] " + args[i]);
		}
	}
}
