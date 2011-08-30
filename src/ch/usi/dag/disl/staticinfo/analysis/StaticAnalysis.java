package ch.usi.dag.disl.staticinfo.analysis;

import java.lang.reflect.Method;

import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.exception.StaticInfoException;

public interface StaticAnalysis {

	// It is mandatory to implement this interface

	// NOTE: all static analysis methods should follow convention:
	// a) static analysis methods does not have parameters
	// b) return value can be only basic type (+String)

	public Object computeStaticData(Method usingMethod, StaticAnalysisData sad)
			throws ReflectionException, StaticInfoException;
}
