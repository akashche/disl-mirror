package ch.usi.dag.disl.staticcontext.cache;

import java.util.HashMap;
import java.util.Map;

import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.staticcontext.StaticAnalysisData;
import ch.usi.dag.disl.util.ReflectionHelper;

public class StaticAnalysisCache {

	private Class<? extends StaticAnalysisData> cacheClass;

	// this could be Map<StaticAnalysisData, Object> but
	// we don't have to cast it for usage if we put there object
	// - valid class is checked already in constructor
	private Map<Object, Object> cachedData = new HashMap<Object, Object>();

	public StaticAnalysisCache(Class<? extends StaticAnalysisData> cacheClass) {
		this.cacheClass = cacheClass;
	}

	private Object createCacheObject(StaticAnalysisData sad) 
		throws ReflectionException {

		// create cache object using "copy" constructor
		return ReflectionHelper.createInstance(cacheClass, sad);
	}

	public Object getCachedResult(StaticAnalysisData sad)
		throws ReflectionException {
		
		return cachedData.get(createCacheObject(sad));
	}

	public void cacheResult(StaticAnalysisData sad, Object result)
		throws ReflectionException {

		cachedData.put(createCacheObject(sad), result);
	}
}
