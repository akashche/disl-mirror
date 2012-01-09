package ch.usi.dag.disl.scope;

public interface Scope {

	public boolean matches(String className, String methodName, String methodDesc);
}
