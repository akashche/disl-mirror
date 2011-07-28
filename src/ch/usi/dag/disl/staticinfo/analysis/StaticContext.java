package ch.usi.dag.disl.staticinfo.analysis;

import ch.usi.dag.disl.staticinfo.analysis.cache.ClassCache;
import ch.usi.dag.disl.staticinfo.analysis.cache.MethodCache;
import ch.usi.dag.disl.util.Constants;

public class StaticContext extends AbstractStaticAnalysis {

	public StaticContext() {
		
		registerCache("getClassName", ClassCache.class);
		registerCache("getMethodName", MethodCache.class);
		registerCache("getFullMethodName", MethodCache.class);
	}
	
	public String thisClassName() {

		return staticAnalysisData.getClassNode().name;
	}
	
	public String thisMethodName() {

		return staticAnalysisData.getMethodNode().name;
	}

	public String thisMethodFullName() {

		return staticAnalysisData.getClassNode().name
				+ Constants.STATIC_ANALYSIS_METHOD_DELIM
				+ staticAnalysisData.getMethodNode().name;
	}
	
	public String thisMethodID() {

		// TODO implement
		return null;
	}
}
