package ch.usi.dag.disl.test2.suite.dispatch2.app;

import ch.usi.dag.dislre.REDispatch;

// Optimally, this class is automatically created on analysis machine
// and redefines during loading the CodeExecuted class on the client vm

// Even more optimally, this is automatically generated native class with same
// functionality
public class CodeExecutedRE {

	private static short ieId = REDispatch.registerMethod(
			"ch.usi.dag.disl.test.dispatch2.CodeExecuted.intEvent");
	
	private static short oeId = REDispatch.registerMethod(
			"ch.usi.dag.disl.test.dispatch2.CodeExecuted.objectEvent");
	
	public static void intEvent(int num) {
		
		REDispatch.analysisStart(ieId);
		
		REDispatch.sendInt(num);
		
		REDispatch.analysisEnd();
	}
	
	public static void objectEvent(Object o) {
		
		REDispatch.analysisStart(oeId);
		
		REDispatch.sendObject(o);
		
		REDispatch.analysisEnd();
	}
}
