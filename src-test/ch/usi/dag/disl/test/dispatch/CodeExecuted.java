package ch.usi.dag.disl.test.dispatch;

import ch.usi.dag.dislreserver.remoteanalysis.RemoteAnalysis;

// NOTE that this class is not static anymore
public class CodeExecuted extends RemoteAnalysis {

	long totalExecutedBytecodes = 0;
	
	public void bytecodesExecuted(int count) {
		totalExecutedBytecodes += count;
	}
	
	public void atExit() {
		System.out.println("Total number of executed bytecodes: "
				+ totalExecutedBytecodes);
	}
}
