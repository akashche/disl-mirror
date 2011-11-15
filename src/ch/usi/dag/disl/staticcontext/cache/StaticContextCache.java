package ch.usi.dag.disl.staticcontext.cache;

import java.util.HashMap;
import java.util.Map;

import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.snippet.Shadow;
import ch.usi.dag.disl.util.ReflectionHelper;

public class StaticContextCache {

	private Class<? extends Shadow> cacheClass;

	// this could be Map<Shadow, Object> but
	// we don't have to cast it for usage if we put there object
	// - valid class is checked already in constructor
	private Map<Object, Object> cachedData = new HashMap<Object, Object>();

	public StaticContextCache(Class<? extends Shadow> cacheClass) {
		this.cacheClass = cacheClass;
	}

	private Object createCacheObject(Shadow sa) 
		throws ReflectionException {

		// create cache object using "copy" constructor
		return ReflectionHelper.createInstance(cacheClass, sa);
	}

	public Object getCachedResult(Shadow sa)
		throws ReflectionException {
		
		return cachedData.get(createCacheObject(sa));
	}

	public void cacheResult(Shadow sa, Object result)
		throws ReflectionException {

		cachedData.put(createCacheObject(sa), result);
	}
}
