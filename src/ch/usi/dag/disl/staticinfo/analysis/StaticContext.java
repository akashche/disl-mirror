package ch.usi.dag.disl.staticinfo.analysis;

import ch.usi.dag.disl.util.Constants;

public class StaticContext extends AbstractStaticAnalysis {

	public String getMethodName() {

		return staticAnalysisData.getMethodNode().name;
	}

	public String getClassName() {

		return staticAnalysisData.getClassNode().name;
	}

	public String getFullName() {

		return staticAnalysisData.getClassNode().name
				+ Constants.STATIC_ANALYSIS_METHOD_DELIM
				+ staticAnalysisData.getMethodNode().name;
	}
}
