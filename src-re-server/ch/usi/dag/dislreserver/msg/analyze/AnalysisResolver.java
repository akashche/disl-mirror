package ch.usi.dag.dislreserver.msg.analyze;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ch.usi.dag.dislreserver.exception.DiSLREServerException;
import ch.usi.dag.dislreserver.exception.DiSLREServerFatalException;
import ch.usi.dag.dislreserver.remoteanalysis.RemoteAnalysis;
import ch.usi.dag.dislreserver.stringcache.StringCache;

public class AnalysisResolver {

	private static final Map<Short, AnalysisMethodHolder> methodMap = 
			new HashMap<Short, AnalysisMethodHolder>();
	
	private static final Map<String, RemoteAnalysis> analysisMap = 
			new HashMap<String, RemoteAnalysis>();
	
	// for fast set access - contains all values from analysisMap
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

	private static AnalysisMethodHolder resolveMethod(long methodStringId)
			throws DiSLREServerException {
		
		try {
		
			final String METHOD_DELIM = ".";
			
			String methodStr = StringCache.resolve(methodStringId);
			
			int classNameEnd = methodStr.lastIndexOf(METHOD_DELIM);
			
			// without METHOD_DELIM
			String className = methodStr.substring(0, classNameEnd);
			String methodName = methodStr.substring(classNameEnd + 1);
			
			// resolve analysis instance
			
			RemoteAnalysis raInst = analysisMap.get(className);
			
			if(raInst == null) {
			
				// resolve class
				Class<?> raClass = Class.forName(className);
				
				// create instance
				raInst = (RemoteAnalysis) raClass.newInstance();
				
				analysisMap.put(className, raInst);
				analysisSet.add(raInst);
			}
			
			// resolve analysis method
			
			Class<?> raClass = raInst.getClass();
			
			Method raMethod = null;
			
			for(Method m : raClass.getMethods()) {
				
				if(m.getName().equals(methodName)) {
					
					// TODO re - check for other methods with same name
					// TODO re - write to docs - RemoteAnalysis.class
					
					raMethod = m;
				}
			}
			
			// TODO re - check that method has after each Class<?> arg, int arg
			
			return new AnalysisMethodHolder(raInst, raMethod);
		}
		catch (ClassNotFoundException e) {
			throw new DiSLREServerException(e);
		} catch (InstantiationException e) {
			throw new DiSLREServerException(e);
		} catch (IllegalAccessException e) {
			throw new DiSLREServerException(e);
		}
	}

	public static AnalysisMethodHolder getMethod(short methodId)
			throws DiSLREServerException {
		
		AnalysisMethodHolder result = methodMap.get(methodId);
		
		if(result == null) {
			throw new DiSLREServerFatalException("Unknown method id: " + methodId);
		}
		
		return result;
	}
	
	public static void registerMethodId(short methodId, long methodStringId)
			throws DiSLREServerException {
		
		methodMap.put(methodId, resolveMethod(methodStringId));
	}
	
	
	
	public static Set<RemoteAnalysis> getAllAnalyses() {
		return analysisSet;
	}
}
