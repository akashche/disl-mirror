package ch.usi.dag.disl.staticcontext;

import java.util.HashMap;
import java.util.Map;

import ch.usi.dag.disl.snippet.Shadow;
import ch.usi.dag.disl.staticcontext.cache.StaticContextCache;

public abstract class AbstractStaticContext implements StaticContext {

	protected Shadow staticContextData;

	private Map<String, StaticContextCache> retValCache =
		new HashMap<String, StaticContextCache>();

	protected <T extends Shadow> void registerCache(
			String methodName, Class<T> keyCacheClass) {

		retValCache.put(methodName, new StaticContextCache(keyCacheClass));
	}

	public void staticContextData(Shadow sa) {

		staticContextData = sa;
	}

}
