package ch.usi.dag.disl.staticinfo.analysis;

import ch.usi.dag.disl.util.Constants;

public class ContextInfo extends AbstractStaticAnalysis {

	public String getMethodName() {

		return staticAnalysisInfo.getMethodNode().name;
	}

	public String getClassName() {

		return staticAnalysisInfo.getClassNode().name;
	}

	public String getFullName() {

		return staticAnalysisInfo.getClassNode().name
				+ Constants.STATIC_ANALYSIS_METHOD_DELIM
				+ staticAnalysisInfo.getMethodNode().name;
	}
}
