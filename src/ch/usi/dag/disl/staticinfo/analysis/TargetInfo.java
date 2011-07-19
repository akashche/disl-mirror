package ch.usi.dag.disl.staticinfo.analysis;

import ch.usi.dag.disl.util.Constants;

public class TargetInfo implements StaticAnalysis {

	public static String getMethodName(StaticAnalysisInfo ai) {

		return ai.getMethodNode().name;
	}

	public static String getClassName(StaticAnalysisInfo ai) {

		return ai.getClassNode().name;
	}

	public static String getFullName(StaticAnalysisInfo ai) {

		return ai.getClassNode().name + Constants.STATIC_ANALYSIS_METHOD_DELIM
				+ ai.getMethodNode().name;
	}
}
