package ch.usi.dag.disl.staticcontext;

import ch.usi.dag.disl.snippet.Shadow;

public interface StaticContext {

	// It is mandatory to implement this interface

	// NOTE: all static context methods should follow convention:
	// a) static context methods does not have parameters
	// b) return value can be only basic type or String

	public void staticContextData(Shadow sa);
}
