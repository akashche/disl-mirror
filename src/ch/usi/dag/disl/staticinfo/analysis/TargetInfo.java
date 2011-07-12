package ch.usi.dag.disl.staticinfo.analysis;

import ch.usi.dag.disl.util.Constants;

public class TargetInfo implements Analysis {

	public static String getMethodName(AnalysisInfo ai) {

		return ai.getMethodNode().name;
	}

	public static String getClassName(AnalysisInfo ai) {

		return ai.getClassNode().name;
	}

	public static String getFullName(AnalysisInfo ai) {

		return ai.getClassNode().name + Constants.ANALYSIS_METHOD_DELIM
				+ ai.getMethodNode().name;
	}
}
