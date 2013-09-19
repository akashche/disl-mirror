package ch.usi.dag.disl.test2.suite.dispatchmp.instr;

import ch.usi.dag.dislre.REDispatch;

// Optimally, this class is automatically created on analysis machine
// and redefines during loading the CodeExecuted class on the client vm

// Even more optimally, this is automatically generated native class with same
// functionality
public class CodeExecutedRE {

	private static short ieId = REDispatch.registerMethod(
			"ch.usi.dag.disl.test.dispatchmp.CodeExecuted.intEvent");
	
	private static short oeId = REDispatch.registerMethod(
			"ch.usi.dag.disl.test.dispatchmp.CodeExecuted.objectEvent");
	
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
