package ch.usi.dag.disl.test.dispatch;

import ch.usi.dag.dislre.REDispatch;

// Optimally, this class is automatically created on analysis machine
// and redefines during loading the CodeExecuted class on the client vm

// Even more optimally, this is automatically generated native class with same
// functionality
public class CodeExecutedRE {

	public static void bytecodesExecuted(int count) {
		REDispatch.analysisStart(1);
		REDispatch.sendInt(count);
		REDispatch.analysisEnd();
	}
}
