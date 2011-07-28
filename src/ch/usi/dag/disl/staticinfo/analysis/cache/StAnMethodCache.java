package ch.usi.dag.disl.staticinfo.analysis.cache;

import java.util.HashMap;
import java.util.Map;

import ch.usi.dag.disl.exception.DiSLException;
import ch.usi.dag.disl.staticinfo.analysis.StaticAnalysisInfo;
import ch.usi.dag.disl.util.ReflectionHelper;

public class StAnMethodCache {

	private Class<? extends StaticAnalysisInfo> cacheClass;

	// this could be Map<StaticAnalysisInfo, Object> but
	// we don't have to cast it for usage if we put there object
	// - valid class is checked already in constructor
	private Map<Object, Object> cachedData = new HashMap<Object, Object>();

	public StAnMethodCache(Class<? extends StaticAnalysisInfo> cacheClass) {
		this.cacheClass = cacheClass;
	}

	private Object createCacheObject(StaticAnalysisInfo sai)
			throws DiSLException {

		// create cache object using "copy" constructor
		return ReflectionHelper.createInstance(cacheClass, sai);
	}

	public Object getCachedResult(StaticAnalysisInfo sai) throws DiSLException {
		
		return cachedData.get(createCacheObject(sai));
	}

	public void cacheResult(StaticAnalysisInfo sai, Object result)
			throws DiSLException {

		cachedData.put(createCacheObject(sai), result);
	}
}
