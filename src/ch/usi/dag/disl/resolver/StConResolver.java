package ch.usi.dag.disl.resolver;

import java.util.HashMap;
import java.util.Map;

import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.staticcontext.StaticContext;
import ch.usi.dag.disl.util.ReflectionHelper;


public class StConResolver {
	
	// NOTE: This is internal DiSL cache. For user static context cache see
	// ch.usi.dag.disl.staticcontext.cache.StaticContextCache

	private static StConResolver instance = null;
	
	// list of static context instances
	// validity of an instance is for whole instrumentation run
	// instances are created lazily when needed
	private Map<Class<?>, Object> staticContextInstances =
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
	
	public static synchronized StConResolver getInstance() {
		
		if (instance == null) {
			instance = new StConResolver();
		}
		return instance;
	}
}
