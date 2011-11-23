package ch.usi.dag.disl.staticcontext.generator;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.usi.dag.disl.coderep.StaticContextMethod;
import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.exception.StaticContextGenException;
import ch.usi.dag.disl.runtimecache.StConCache;
import ch.usi.dag.disl.snippet.ProcInvocation;
import ch.usi.dag.disl.snippet.Shadow;
import ch.usi.dag.disl.snippet.Snippet;
import ch.usi.dag.disl.snippet.processor.ProcMethod;
import ch.usi.dag.disl.staticcontext.StaticContext;
import ch.usi.dag.disl.util.Constants;

public class SCGenerator {

	class StaticContextKey {

		private Shadow shadow;
		private String methodID;

		public StaticContextKey(Shadow shadow, String methodID) {
			super();
			this.shadow = shadow;
			this.methodID = methodID;
		}

		@Override
		public int hashCode() {

			final int prime = 31;
			int result = 1;

			result = prime * result + getOuterType().hashCode();

			result = prime * result
					+ ((shadow == null) ? 0 : shadow.hashCode());

			result = prime * result
					+ ((methodID == null) ? 0 : methodID.hashCode());

			return result;
		}

		@Override
		public boolean equals(Object obj) {

			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			StaticContextKey other = (StaticContextKey) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;

			if (shadow == null) {
				if (other.shadow != null) {
					return false;
				}
			} else if (!shadow.equals(other.shadow)) {
				return false;
			}

			if (methodID == null) {
				if (other.methodID != null) {
					return false;
				}
			} else if (!methodID.equals(other.methodID)) {
				return false;
			}

			return true;
		}

		private SCGenerator getOuterType() {
			return SCGenerator.this;
		}
	}

	Map<StaticContextKey, Object> staticInfoData = 
			new HashMap<StaticContextKey, Object>();

	public SCGenerator(Map<Snippet, List<Shadow>> snippetMarkings)
			throws ReflectionException, StaticContextGenException {

		computeStaticInfo(snippetMarkings);
	}
	
	private StaticContextKey createStaticInfoKey(Shadow shadow,
			String infoClass, String infoMethod) {
		
		String methodID = infoClass + Constants.STATIC_CONTEXT_METHOD_DELIM
				+ infoMethod;
		
		return new StaticContextKey(shadow, methodID);
	}
	
	public boolean contains(Shadow shadow, String infoClass,
			String infoMethod) {
		
		StaticContextKey sck = 
			createStaticInfoKey(shadow, infoClass, infoMethod);
		
		return staticInfoData.containsKey(sck);
	}

	public Object get(Shadow shadow, String infoClass, String infoMethod) {

		StaticContextKey sck = 
			createStaticInfoKey(shadow, infoClass, infoMethod);
		
		return staticInfoData.get(sck);
	}

	// Call static context for each snippet and each marked region and create
	// a static info values
	private void computeStaticInfo(
			Map<Snippet, List<Shadow>> snippetMarkings)
			throws ReflectionException, StaticContextGenException {

		for (Snippet snippet : snippetMarkings.keySet()) {

			for (Shadow shadow : snippetMarkings.get(snippet)) {

				// compute static data for snippet and all processors
				// static data for snippets and processors can be evaluated
				// and stored together
				
				Set<StaticContextMethod> stConMethods = 
						snippet.getCode().getStaticContexts();
				
				// add static contexts from all processors
				for(ProcInvocation pi :
					snippet.getCode().getInvokedProcessors().values()) {
					
					// add static contexts from all processor methods
					for(ProcMethod pm : pi.getProcessor().getMethods()) {
						
						// add to the pool
						stConMethods.addAll(pm.getCode().getStaticContexts());
					}
				}
				
				// process all static context methods
				
				for (StaticContextMethod stConMth : stConMethods) {

					// get static context instance
					StaticContext scInst = 
							StConCache.getInstance().getStaticContextInstance(
									stConMth.getReferencedClass());
					
					// populate static context instance with data
					scInst.staticContextData(shadow);
					
					// get static data by invoking static context method
					Object result = invokeStaticContextMethod(
							stConMth.getMethod(), scInst);
							

					// store the result
					setSI(shadow, stConMth.getId(), result);
				}
			}
		}
	}
	
	private Object invokeStaticContextMethod(Method staticContextMethod,
			Object methodObject) throws StaticContextGenException {
		
		try {

			// invoke static context method and return result
			return staticContextMethod.invoke(methodObject);

		} catch (Exception e) {
			throw new StaticContextGenException(
					"Invocation of static context method "
							+ staticContextMethod.getName() + " failed", e);
		}
	}
	
	// TODO ! static context - introduce caching 
	/*
	
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
	
	*/

	private void setSI(Shadow shadow, String methodID, Object value) {

		staticInfoData.put(new StaticContextKey(shadow, methodID), value);
	}
}
