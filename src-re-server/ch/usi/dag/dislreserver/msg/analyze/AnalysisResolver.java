package ch.usi.dag.dislreserver.msg.analyze;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ch.usi.dag.disl.test.dispatch.CodeExecuted;
import ch.usi.dag.dislreserver.exception.DiSLREServerFatalException;
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
			throw new DiSLREServerFatalException("Unknow method id");
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
		// TODO re - after is not hardcoded change build
		// in compile-dislre-server
		//  - no dependency
		// in compile-test
		//  - ,compile-dislre-server,compile-dislre-dispatch should be added to dependences
		//  - no first javac
		
		try {
			Object i = CodeExecuted.class.newInstance();
			
			Method m1 = CodeExecuted.class.getMethod("bytecodesExecuted", int.class);
			registerMethod(1, new AnalysisMethodHolder((RemoteAnalysis)i, m1));
			
			Method m2 = CodeExecuted.class.getMethod("testingBasic",
					boolean.class, byte.class, char.class, short.class,
					int.class, long.class, float.class, double.class);
			registerMethod(2, new AnalysisMethodHolder((RemoteAnalysis)i, m2));
			
			Method m3 = CodeExecuted.class.getMethod("testingAdvanced",
					String.class, Object.class, Class.class, int.class);
			registerMethod(3, new AnalysisMethodHolder((RemoteAnalysis)i, m3));
			
			Method m4 = CodeExecuted.class.getMethod("testingAdvanced2",
					Object.class, Object.class, Object.class, Object.class,
					Class.class, int.class, Class.class, int.class,
					Class.class, int.class, Class.class, int.class);
			registerMethod(4, new AnalysisMethodHolder((RemoteAnalysis)i, m4));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
