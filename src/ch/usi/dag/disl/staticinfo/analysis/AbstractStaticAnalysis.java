package ch.usi.dag.disl.staticinfo.analysis;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.exception.StaticInfoException;
import ch.usi.dag.disl.staticinfo.analysis.cache.StaticAnalysisCache;

abstract public class AbstractStaticAnalysis implements StaticAnalysis {

	protected StaticAnalysisData staticAnalysisData;

	private Map<String, StaticAnalysisCache> retValCache =
		new HashMap<String, StaticAnalysisCache>();

	protected <T extends StaticAnalysisData> void registerCache(
			String methodName, Class<T> keyCacheClass) {

		retValCache.put(methodName, new StaticAnalysisCache(keyCacheClass));
	}

	@Override
	public Object computeStaticData(Method usingMethod, StaticAnalysisData sad)
			throws ReflectionException, StaticInfoException {

		staticAnalysisData = sad;

		// NOTE: default cache
		// some default cache is not needed because for each marked region,
		// the computation is called only once

		// resolve specific method cache
		StaticAnalysisCache cache = retValCache.get(usingMethod.getName());

		// resolve cached data
		if (cache != null) {

			Object result = cache.getCachedResult(sad);

			// return cache hit
			if (result != null) {
				return result;
			}
		}

		// if cache wasn't hit...
		try {

			// ... invoke static analysis method
			Object result = usingMethod.invoke(this);

			if (cache != null) {
				// ... cache result
				cache.cacheResult(sad, result);
			}

			// ... return result
			return result;

		} catch (Exception e) {
			throw new StaticInfoException(
					"Invocation of static analysis method "
							+ usingMethod.getName() + " failed", e);
		}
	}

}
