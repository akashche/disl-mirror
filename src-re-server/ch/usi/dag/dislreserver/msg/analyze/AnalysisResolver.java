package ch.usi.dag.dislreserver.msg.analyze;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
            Class<? extends RemoteAnalysis> clazz = getClass("ch.usi.dag.disl.test.dispatch.CodeExecuted");
		    RemoteAnalysis instance = clazz.newInstance();

			Method m1 = clazz.getMethod("bytecodesExecuted", int.class);
			registerMethod(1, new AnalysisMethodHolder(instance, m1));

			Method m2 = clazz.getMethod("testingBasic",
					boolean.class, byte.class, char.class, short.class,
					int.class, long.class, float.class, double.class);
			registerMethod(2, new AnalysisMethodHolder(instance, m2));

			Method m3 = clazz.getMethod("testingAdvanced",
					String.class, Object.class, Class.class, int.class);
			registerMethod(3, new AnalysisMethodHolder(instance, m3));

			Method m4 = clazz.getMethod("testingAdvanced2",
					Object.class, Object.class, Object.class, Object.class,
					Class.class, int.class, Class.class, int.class,
					Class.class, int.class, Class.class, int.class);
			registerMethod(4, new AnalysisMethodHolder(instance, m4));
        } catch (Exception e) {
            System.err.println("[AnalysisResolver.<clinit>] " + e);
            System.err.flush();
        }

		try {
            Class<? extends RemoteAnalysis> clazz = getClass("ch.usi.dag.disl.example.codeCov.CodeCoverage");
			RemoteAnalysis instance2 = clazz.newInstance();

			Method m5 = clazz.getMethod("onConstructor", Class.class);
			registerMethod(5, new AnalysisMethodHolder(instance2, m5));

			Method m6 = clazz.getMethod("onBB", String.class, int.class);
			registerMethod(6, new AnalysisMethodHolder(instance2, m6));
		} catch (Exception e) {
		    System.err.println("[AnalysisResolver.<clinit>] " + e);
		    System.err.flush();
		}
	}

	private static Class<? extends RemoteAnalysis> getClass(String className) throws ClassNotFoundException, ClassCastException {
	    Class<?> clazz = Class.forName(className);
	    return clazz.asSubclass(RemoteAnalysis.class);
	}
}
