package ch.usi.dag.dislreserver.msg.analyze;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ch.usi.dag.disl.test.dispatch.CodeExecuted;
import ch.usi.dag.dislreserver.remoteanalysis.RemoteAnalysis;

public class AnalysisResolver {

	private static final Map<Integer, AnalysisMethodHolder> methodMap = 
			new HashMap<Integer, AnalysisMethodHolder>();
	
	private static final Set<RemoteAnalysis> analysisSet = 
			new HashSet<RemoteAnalysis>();
	
	public static class AnalysisMethodHolder {
		
		RemoteAnalysis analysisInstance;
		Method analysisMethod;
		
		public AnalysisMethodHolder(RemoteAnalysis analysisInstance,
				Method analysisMethod) {
			super();
			this.analysisInstance = analysisInstance;
			this.analysisMethod = analysisMethod;
		}

		public RemoteAnalysis getAnalysisInstance() {
			return analysisInstance;
		}
		
		public Method getAnalysisMethod() {
			return analysisMethod;
		}
	}
	
	public static AnalysisMethodHolder getMethod(int methodId) {
		
		AnalysisMethodHolder result = methodMap.get(methodId);
		
		if(result == null) {
			// TODO re - different except
			throw new RuntimeException("Unknow method id");
		}
		
		return result;
	}
	
	public static Set<RemoteAnalysis> getAllAnalyses() {
		return analysisSet;
	}
	
	public static void registerMethod(int methodId, 
			AnalysisMethodHolder analysisMethod) {

		// register method
		methodMap.put(methodId, analysisMethod);
		
		// register analysis instance
		analysisSet.add(analysisMethod.getAnalysisInstance());
	}
	
	static {
		
		// TODO re - should not be hardcoded
		
		try {
			Method m = CodeExecuted.class.getMethod("bytecodesExecuted", int.class);
			Object i = CodeExecuted.class.newInstance();
			registerMethod(1, new AnalysisMethodHolder((RemoteAnalysis)i, m));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
