package ch.usi.dag.disl.test.dispatch2;

import ch.usi.dag.dislre.jb.REDispatchJ;

// Optimally, this class is automatically created on analysis machine
// and redefines during loading the CodeExecuted class on the client vm

// Even more optimally, this is automatically generated native class with same
// functionality
public class CodeExecutedRE {

	private static short ieId = REDispatchJ.registerMethod(
			"ch.usi.dag.disl.test.dispatch2.CodeExecuted.intEvent");
	
	private static short oeId = REDispatchJ.registerMethod(
			"ch.usi.dag.disl.test.dispatch2.CodeExecuted.objectEvent");
	
	public static void intEvent(int num) {
		
		REDispatchJ.analysisStart(ieId);
		
		REDispatchJ.sendInt(num);
		
		REDispatchJ.analysisEnd();
	}
	
	public static void objectEvent(Object o) {
		
		REDispatchJ.analysisStart(oeId);
		
		REDispatchJ.sendObject(o);
		
		REDispatchJ.analysisEnd();
	}
}
