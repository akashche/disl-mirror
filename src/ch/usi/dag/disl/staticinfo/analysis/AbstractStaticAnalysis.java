package ch.usi.dag.disl.staticinfo.analysis;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import ch.usi.dag.disl.exception.DiSLException;
import ch.usi.dag.disl.exception.StaticAnalysisException;
import ch.usi.dag.disl.staticinfo.analysis.cache.StAnMethodCache;

abstract public class AbstractStaticAnalysis implements StaticAnalysis {

	protected StaticAnalysisData staticAnalysisInfo;
	
	private Map<String, StAnMethodCache> retValCache = 
		new HashMap<String, StAnMethodCache>();
	
	protected <T extends StaticAnalysisData> void
			registerCache(String methodName, Class<T> keyCacheClass) {

		retValCache.put(methodName, new StAnMethodCache(keyCacheClass));
	}
	
	@Override
	public Object computeStaticData(Method usingMethod, StaticAnalysisData sad)
			throws DiSLException {

		staticAnalysisInfo = sad;

		// NOTE: default method cache
		// default method cache is not needed because for each marking,
		// the computation is called only once
		
		// resolve specific method cache
		StAnMethodCache methodCache = retValCache.get(usingMethod.getName());
		
		// resolve cached data
		if(methodCache != null) {

			Object result = methodCache.getCachedResult(sad);
			
			// return cache hit
			if(result != null) {
				return result;
			}
		}
		
		// if cache wasn't hit...
		try {

			// ... invoke static analysis method
			Object result = usingMethod.invoke(this);

			if(methodCache != null) {
				// ... cache result
				methodCache.cacheResult(sad, result);
			}
			
			// ... return result
			return result;
			
		} catch (Exception e) {
			throw new StaticAnalysisException(
					"Invocation of static analysis method " +
					usingMethod.getName() + " failed", e);
		}
	}

}
