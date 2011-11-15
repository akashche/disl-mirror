package ch.usi.dag.disl.staticcontext;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.exception.StaticContextGenException;
import ch.usi.dag.disl.staticcontext.cache.StaticContextCache;

abstract public class AbstractStaticContext implements StaticContext {

	protected Shadow staticContextData;

	private Map<String, StaticContextCache> retValCache =
		new HashMap<String, StaticContextCache>();

	protected <T extends Shadow> void registerCache(
			String methodName, Class<T> keyCacheClass) {

		retValCache.put(methodName, new StaticContextCache(keyCacheClass));
	}

	@Override
	public Object computeStaticData(Method usingMethod, Shadow sa)
			throws ReflectionException, StaticContextGenException {

		staticContextData = sa;

		// NOTE: default cache
		// some default cache is not needed because for each marked region,
		// the computation is called only once

		// resolve specific method cache
		StaticContextCache cache = retValCache.get(usingMethod.getName());

		// resolve cached data
		if (cache != null) {

			Object result = cache.getCachedResult(sa);

			// return cache hit
			if (result != null) {
				return result;
			}
		}

		// if cache wasn't hit...
		try {

			// ... invoke static context method
			Object result = usingMethod.invoke(this);

			if (cache != null) {
				// ... cache result
				cache.cacheResult(sa, result);
			}

			// ... return result
			return result;

		} catch (Exception e) {
			throw new StaticContextGenException(
					"Invocation of static context method "
							+ usingMethod.getName() + " failed", e);
		}
	}

}
