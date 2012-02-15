package ch.usi.dag.disl.test.dispatch;

import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.marker.BasicBlockMarker;

public class DiSLClass {
	
	@After(marker = BasicBlockMarker.class, scope = "TargetClass.*")
	public static void invokedInstr(CodeLengthSC clsc) {
		CodeExecutedRE.bytecodesExecuted(clsc.codeSize());
	}
}
