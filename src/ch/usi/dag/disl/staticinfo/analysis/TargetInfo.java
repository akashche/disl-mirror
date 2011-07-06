package ch.usi.dag.disl.staticinfo.analysis;

public class TargetInfo implements Analysis {

	public static String getMethodName(AnalysisInfo ai) {
		
		return ai.getMethodNode().name;
	}
}
