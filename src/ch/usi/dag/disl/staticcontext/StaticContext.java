package ch.usi.dag.disl.staticcontext;

import java.lang.reflect.Method;

import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.exception.StaticContextGenException;

public interface StaticContext {

	// It is mandatory to implement this interface

	// NOTE: all static context methods should follow convention:
	// a) static context methods does not have parameters
	// b) return value can be only basic type (+String)

	public Object computeStaticData(Method usingMethod, Shadow sa)
			throws ReflectionException, StaticContextGenException;
}
