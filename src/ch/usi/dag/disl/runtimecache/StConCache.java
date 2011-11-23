package ch.usi.dag.disl.runtimecache;

import java.util.HashMap;
import java.util.Map;

import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.staticcontext.StaticContext;
import ch.usi.dag.disl.util.ReflectionHelper;


public class StConCache {
	
	// NOTE: This is internal DiSL cache. For user static context cache see
	// ch.usi.dag.disl.staticcontext.cache.StaticContextCache

	// list of static context instances
	// validity of an instance is for whole instrumentation run
	// instances are created lazily when needed
	Map<Class<?>, Object> staticContextInstances =
			new HashMap<Class<?>, Object>();
	
	public synchronized StaticContext getStaticContextInstance(
			Class<?> staticContextClass) throws ReflectionException {
		
		// get static context instance from cache
		Object scInst = staticContextInstances.get(staticContextClass);

		// ... or create new one
		if (scInst == null) {

			scInst = ReflectionHelper.createInstance(staticContextClass);

			// and store for later use
			staticContextInstances.put(staticContextClass, scInst);
		}

		// recast context object to interface
		return (StaticContext) scInst;
	}
	
	private static StConCache instance = null;
	
	public static synchronized StConCache getInstance() {
		
		if (instance == null) {
			instance = new StConCache();
		}
		return instance;
	}
}
