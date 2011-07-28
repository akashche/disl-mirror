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
	
	public String getClassName() {

		return staticAnalysisData.getClassNode().name;
	}
	
	public String getMethodName() {

		return staticAnalysisData.getMethodNode().name;
	}

	public String getFullMethodName() {

		return staticAnalysisData.getClassNode().name
				+ Constants.STATIC_ANALYSIS_METHOD_DELIM
				+ staticAnalysisData.getMethodNode().name;
	}
}
