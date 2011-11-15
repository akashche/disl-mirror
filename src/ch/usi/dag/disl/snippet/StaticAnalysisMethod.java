package ch.usi.dag.disl.snippet;

import java.lang.reflect.Method;

public class StaticAnalysisMethod {

	Method method;
	Class<?> referencedClass;
	
	public StaticAnalysisMethod(Method method, Class<?> referencedClass) {
		super();
		this.method = method;
		this.referencedClass = referencedClass;
	}

	public Method getMethod() {
		return method;
	}

	public Class<?> getReferencedClass() {
		return referencedClass;
	}
}
