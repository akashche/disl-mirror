package ch.usi.dag.disl.staticcontext;

import ch.usi.dag.disl.snippet.Shadow;

/**
 * Every static context class has to implement this interface.
 * 
 * All static context methods should follow convention:
 * a) static context methods does not have parameters
 * b) return value can be only basic type or String
 */
public interface StaticContext {

	/**
	 * Receives static context data. Call to this method precedes a static
	 * context method invocation.
	 */
	public void staticContextData(Shadow sa);
}
