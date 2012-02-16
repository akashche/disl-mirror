package ch.usi.dag.disl.test.dispatch;

import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.marker.BasicBlockMarker;
import ch.usi.dag.disl.marker.BodyMarker;

public class DiSLClass {
	
	@After(marker = BasicBlockMarker.class, scope = "TargetClass.*")
	public static void invokedInstr(CodeLengthSC clsc) {
		
		CodeExecutedRE.bytecodesExecuted(clsc.codeSize());
	}
	
	@After(marker = BodyMarker.class, scope = "TargetClass.main()")
	public static void testing() {
		
		CodeExecutedRE.testingBasic(true, (byte) 125, 'š', (short) 50000,
				100000, 10000000000L, 1.5F, 2.5);
		
		CodeExecutedRE.testingAdvanced("ěščřžýáíé", new Object(), Object.class, 0);
	}
}
